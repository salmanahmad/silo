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

import java.util.Stack;
import java.util.HashMap;

import org.objectweb.asm.commons.GeneratorAdapter;

public class CompilationFrame {

    public final GeneratorAdapter generator;

    public final Class outputClass;
    public final Stack<Class> operandStack;
    public final HashMap<Symbol, Integer> locals;
    public final HashMap<Symbol, Class> localTypes;

    public CompilationFrame(GeneratorAdapter generator, Class outputClass) {
        this.generator = generator;
        this.outputClass = outputClass;

        this.operandStack = new Stack<Class>();
        this.locals = new HashMap<Symbol, Integer>();
        this.localTypes = new HashMap<Symbol, Class>();
    }
}
