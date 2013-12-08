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

public class LiteralDouble implements Expression {

    public final double value;

    public LiteralDouble(double value) {
        this.value = value;
    }

    public void emit(CompilationContext context) {
        context.currentFrame().operandStack.push(Double.TYPE);
        context.currentFrame().generator.push(value);
    }
}
