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

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

public class MathOperation implements Expression {

    public final Expression e1;
    public final Expression e2;
    public int operation;

    public MathOperation(Expression e1, Expression e2, int operation) {
        this.e1 = e1;
        this.e2 = e2;
        this.operation = operation;
    }

    public void emit(CompilationContext context, GeneratorAdapter generator) {
        this.e1.emit(context, generator);
        this.e2.emit(context, generator);

        // TODO: Implicit conversion rules

        generator.math(this.operation, Type.INT_TYPE);

        context.operandStack.pop();
        context.operandStack.pop();
        context.operandStack.push(Type.INT_TYPE);
    }
}
