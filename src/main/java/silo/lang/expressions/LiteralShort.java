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

public class LiteralShort implements Expression {

    public final short value;

    public LiteralShort(short value) {
        this.value = value;
    }

    public LiteralShort(Short value) {
        this.value = value.shortValue();
    }

    public Class type(CompilationContext context) {
        return Short.TYPE;
    }

    public Object scaffold(CompilationContext context) {
        return value;
    }

    public void emit(CompilationContext context) {
        context.currentFrame().operandStack.push(Short.TYPE);
        context.currentFrame().generator.push(value);
    }
}
