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

public class LiteralCharacter implements Expression {

    public final char value;

    public LiteralCharacter(char value) {
        this.value = value;
    }

    public LiteralCharacter(Character value) {
        this.value = value.charValue();
    }

    public void emit(CompilationContext context) {
        context.currentFrame().operandStack.push(Character.TYPE);
        context.currentFrame().generator.push(value);
    }
}
