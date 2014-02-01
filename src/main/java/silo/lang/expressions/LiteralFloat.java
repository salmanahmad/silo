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

public class LiteralFloat implements Expression {

    public final float value;

    public LiteralFloat(float value) {
        this.value = value;
    }

    public LiteralFloat(Float value) {
        this.value = value.floatValue();
    }

    public Class type(CompilationContext context) {
        return Float.TYPE;
    }

    public Object scaffold(CompilationContext context) {
        return value;
    }

    public void emit(CompilationContext context) {
        context.currentFrame().operandStack.push(Float.TYPE);
        context.currentFrame().generator.push(value);
    }
}
