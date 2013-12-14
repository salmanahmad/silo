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
import org.objectweb.asm.commons.GeneratorAdapter;

public class CompilationFrame {

    public final GeneratorAdapter generator;
    public final Stack<Class> operandStack;
    public final Class outputClass;

    public CompilationFrame(GeneratorAdapter generator, Class outputClass) {
        this.generator = generator;
        this.operandStack = new Stack<Class>();
        this.outputClass = outputClass;
    }
}
