/*
 *
 *  Copyright 2012 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.lang;

import org.objectweb.asm.Type;

import java.util.Stack;

public class CompilationContext {

    Runtime runtime;

    public Stack<Type> operandStack = new Stack<Type>();
    int uniqueIdentifierCounter = 0;

    public CompilationContext(Runtime runtime) {
        this.runtime = runtime;
    }

    public Runtime runtime() {
        return runtime;
    }

    public Symbol uniqueIdentifier(String tag) {
        uniqueIdentifierCounter += 1;
        return new Symbol("__" + tag + "__" + uniqueIdentifierCounter);
    }

}
