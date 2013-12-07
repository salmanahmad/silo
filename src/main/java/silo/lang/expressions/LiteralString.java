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

package silo.lang.expressions;

import silo.lang.*;

import org.objectweb.asm.commons.GeneratorAdapter;

public class LiteralString implements Expression {

    public final String string;

    public LiteralString(String string) {
        this.string = string;
    }

    public void emit(CompilationContext context, GeneratorAdapter generator) {
        context.operandStack.push(String.class);
        generator.push(string);
    }
}
