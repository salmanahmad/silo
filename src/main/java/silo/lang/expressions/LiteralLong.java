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

public class LiteralLong implements Expression {

    public final long value;

    public LiteralLong(long value) {
        this.value = value;
    }

    public LiteralLong(Long value) {
        this.value = value.longValue();
    }

    public Class type(CompilationContext context) {
        return Long.TYPE;
    }

    public void emitDeclaration(CompilationContext context) {
        return;
    }

    public void emit(CompilationContext context) {
        context.currentFrame().operandStack.push(Long.TYPE);
        context.currentFrame().generator.push(value);
    }
}
