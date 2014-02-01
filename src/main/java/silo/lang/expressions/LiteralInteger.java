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

public class LiteralInteger implements Expression {

    public final int value;

    public LiteralInteger(int value) {
        this.value = value;
    }

    public LiteralInteger(Integer value) {
        this.value = value.intValue();
    }

    public Class type(CompilationContext context) {
        return Integer.TYPE;
    }

    public Object scaffold(CompilationContext context) {
        return value;
    }

    public void emit(CompilationContext context) {
        context.currentFrame().operandStack.push(Integer.TYPE);
        context.currentFrame().generator.push(value);
    }
}
