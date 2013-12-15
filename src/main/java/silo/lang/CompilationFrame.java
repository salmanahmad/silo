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

    private int nextLocal = 0;

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

    public int newLocal(Symbol name, Class type) {
        int local = nextLocal;

        if(locals.containsKey(name)) {
            throw new RuntimeException("Duplicated variable name.");
        }

        locals.put(name, local);
        localTypes.put(name, type);

        if(type.equals(Double.TYPE) || type.equals(Long.TYPE)) {
            nextLocal += 2;
        } else {
            nextLocal += 1;
        }

        return local;
    }
}
