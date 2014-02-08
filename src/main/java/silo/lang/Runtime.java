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

import java.util.Vector;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class Runtime {

    // TODO: Should this handle and manage which files have been required to avoid redefining and compiling stuff?
    // Perhaps I should have a utility helper method called "Runtime.require" along side Runtime.compile() and Runtime.execute().
    // Runtime.compile(String) should be provided in some manner...

    public final RuntimeClassLoader loader;

    CompilationContext compilationContext;

    public Runtime() {
        this(new RuntimeClassLoader());
    }

    public Runtime(RuntimeClassLoader loader) {
        this.loader = loader;
        this.compilationContext = new CompilationContext(this);
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
            Object[] actualArgs = new Object[args.length + 1];
            System.arraycopy(args, 0, actualArgs, 1, args.length);
            actualArgs[0] = context;
            args = actualArgs;

            if(Function.isVarArgs(klass)) {
                args = Function.convertArgsToVarArgs(klass, args);
            }

            return ((Function)klass.newInstance()).methodHandle().invoke(null, args);
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

        return compilationContext.classes;
    }

}