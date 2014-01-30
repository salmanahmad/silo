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

public class LiteralByte implements Expression {

    public final byte value;

    public LiteralByte(byte value) {
        this.value = value;
    }

    public LiteralByte(Byte value) {
        this.value = value.byteValue();
    }

    public Class type(CompilationContext context) {
        return Byte.TYPE;
    }

    public void emitDeclaration(CompilationContext context) {
        return;
    }

    public void emit(CompilationContext context) {
        context.currentFrame().operandStack.push(Byte.TYPE);
        context.currentFrame().generator.push(value);
    }
}
