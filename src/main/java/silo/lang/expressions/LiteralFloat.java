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

package silo.lang;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

public class LiteralFloat implements Expression {

    public final float value;

    public LiteralFloat(float value) {
        this.value = value;
    }

    public void emit(CompilationContext context, GeneratorAdapter generator) {
        context.operandStack.push(Type.FLOAT_TYPE);
        generator.push(value);
    }
}