/*
 *
 *  Copyright 2013 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.lang;

import silo.lang.compiler.Parser;
import silo.lang.compiler.Compiler;

import java.util.UUID;
import java.util.Vector;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

// TODO: move Fiber into silo.lang
import silo.core.fiber.Fiber;

public class Runtime {

    // TODO: Should this handle and manage which files have been required to avoid redefining and compiling stuff?
    // Perhaps I should have a utility helper method called "Runtime.require" along side Runtime.compile() and Runtime.execute().
    // Runtime.compile(String) should be provided in some manner...

    public final RuntimeClassLoader loader;
    CompilationContext compilationContext;

    public ConcurrentHashMap<String, Actor> actors;
    public ExecutorService actorExecutor;
    public ExecutorService backgroundExecutor;

    public ConcurrentHashMap<String, Object> registry;

    public Runtime() {
        //this(new RuntimeClassLoader(), java.lang.Runtime.getRuntime().availableProcessors() * 2);
        this(new RuntimeClassLoader(), java.lang.Runtime.getRuntime().availableProcessors());
    }

    public Runtime(RuntimeClassLoader loader, int nThreads) {
        this.loader = loader;
        this.compilationContext = new CompilationContext(this);

        this.actors = new ConcurrentHashMap<String, Actor>();
        //this.actorExecutor = Executors.newFixedThreadPool(nThreads);
        this.actorExecutor = new ForkJoinPool(nThreads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        this.backgroundExecutor = Executors.newCachedThreadPool();

        this.registry = new ConcurrentHashMap<String, Object>();
    }

    public void shutdown() {
        actorExecutor.shutdownNow();
        backgroundExecutor.shutdownNow();
    }

    public Actor spawn(String fullyQualifiedFunctionName, Object ... arguments) {
        return spawn(UUID.randomUUID().toString(), fullyQualifiedFunctionName, arguments);
    }

    public Actor spawn(String address, String fullyQualifiedFunctionName, Object ... arguments) {
        try {
            // TODO: What if klass is a type and not a function?
            Class klass = loader.loadClass(fullyQualifiedFunctionName);
            return spawn(address, (Function)(klass.newInstance()), arguments);
        } catch(ClassNotFoundException e) {
            throw new RuntimeException("Could not load function: " + fullyQualifiedFunctionName);
        } catch(Exception e) {
            // TODO: Better error handling.
            // TODO: This is potentially bad.
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Actor spawn(Function function, Object ... arguments) {
        return spawn(UUID.randomUUID().toString(), function, arguments);
    }

    public Actor spawn(String address, Function function, Object ... arguments) {
        Actor actor = new Actor(this, address, new Fiber(function, arguments));
        this.actors.put(address, actor);
        actor.schedule(false);

        return actor;
    }

    public Actor spawnFork(Function function, Object ... arguments) {
        return spawnFork(UUID.randomUUID().toString(), function, arguments);
    }

    public Actor spawnFork(String address, Function function, Object ... arguments) {
        Actor actor = new Actor(this, address, new Fiber(function, arguments));
        this.actors.put(address, actor);
        actor.schedule(true);

        return actor;
    }

    public Object eval(Node node) {
        Class klass = this.compile(node).lastElement();
        return eval(klass);
    }

    public Object eval(String fullyQualifiedFunctionName, Object... args) {
        try {
            // TODO: What if klass is a type and not a function?
            Class klass = loader.loadClass(fullyQualifiedFunctionName);
            return eval(klass, args);
        } catch(ClassNotFoundException e) {
            throw new RuntimeException("Could not load function: " + fullyQualifiedFunctionName);
        }
    }

    public Object eval(Class klass, Object... args) {
        return Runtime.doEval(klass, args);
    }

    public static Object doEval(Class klass, Object... args) {
        return doEval(klass, new ExecutionContext(), args);
    }

    public static Object doEval(Class klass, ExecutionContext context, Object... args) {
        try {
            // TODO: I have to do beginCall and endCall, don't I???
            // In fact, I really need to fundamentally re-think this "eval"
            // method. Perhaps beginCall and endCall belong in a "resume" method
            // and "eval" is just a utility method?
            // If not, then I also need an eval in which the execution context is PART
            // of the args[] array. Perhaps, "Object evalWithContext(Class, Object...)"

            Object[] actualArgs = new Object[args.length + 1];
            System.arraycopy(args, 0, actualArgs, 1, args.length);
            actualArgs[0] = context;
            args = actualArgs;

            if(Function.isVarArgs(klass)) {
                args = Function.convertArgsToVarArgs(klass, args);
            }

            context.beginCall();
            Object output = ((Function)klass.newInstance()).methodHandle().invoke(null, args);
            context.endCall();

            return output;
        } catch(java.lang.reflect.InvocationTargetException e) {
            Throwable t = e.getCause();

            if(t instanceof RuntimeException) {
                throw (RuntimeException)t;
            } else {
                throw new RuntimeException(t);
            }
        } catch(Exception e) {
            // TODO: Better error handling.
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public CompilationContext contextByCompiling(String source) {
        return contextByCompiling(null, source);
    }

    public CompilationContext contextByCompiling(String fileName, String source) {
        compile(Parser.parse(fileName, source));
        return compilationContext;
    }

    public Vector<Class> compile(Node node) {
        Node program = new Node(new Symbol("function"),
            new Node(new Symbol("do"),
                node
            )
        );

        program.meta = node.meta;
        compilationContext.clear();
        Compiler.compile(compilationContext, program);

        Vector<Class> userClasses = new Vector<Class>();
        for(Class klass : compilationContext.classes) {
            if(!klass.getName().startsWith("silo.lang.rt.frames")) {
                userClasses.add(klass);
            }
        }

        //return compilationContext.classes;
        return userClasses;
    }

    public Object expandCode(Object code) {
        Node program = new Node(new Symbol("function"),
            new Node(new Symbol("do"),
                code
            )
        );

        if(code instanceof Node) {
            program.meta = ((Node)code).meta;
        }

        return Compiler.expandCode(compilationContext, program);
    }

    public Vector<Class> compileExpandedCode(Object code) {
        return Compiler.compileExpandedCode(compilationContext, code);
    }
}