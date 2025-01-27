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

public class LiteralNull implements Expression {

    public LiteralNull() {
    }

    public Class type(CompilationContext context) {
        return Null.class;
    }

    public Object scaffold(CompilationContext context) {
        return null;
    }

    public void emit(CompilationContext context) {
        context.currentFrame().operandStack.push(Null.class);
        context.currentFrame().generator.push((String)null);
    }
}
