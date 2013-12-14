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

    public Object eval(Class klass) {
        try {
            return ((Function)klass.newInstance()).methodHandle().invoke(null);
        } catch(Exception e) {
            // TODO: Better error handling.
            e.printStackTrace();
            throw new RuntimeException("Error!");
        }
    }

    public Vector<Class> compile(Node node) {
        Node program = new Node(new Symbol("function"),
            new Node(new Symbol("do"),
                node
            )
        );

        compilationContext.clear();
        Compiler.compile(compilationContext, program);

        return compilationContext.classes;
    }

}