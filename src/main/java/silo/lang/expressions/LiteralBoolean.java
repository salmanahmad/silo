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

public class LiteralBoolean implements Expression {

    public final boolean value;

    public LiteralBoolean(boolean value) {
        this.value = value;
    }

    public LiteralBoolean(Boolean value) {
        this.value = value.booleanValue();
    }

    public Class type(CompilationContext context) {
        return Boolean.TYPE;
    }

    public void emitDeclaration(CompilationContext context) {
        return;
    }

    public void emit(CompilationContext context) {
        context.currentFrame().operandStack.push(Boolean.TYPE);
        context.currentFrame().generator.push(value);
    }
}
