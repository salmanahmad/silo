/*
 *
 *  Copyright 2014 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.lang.expressions;

import silo.lang.*;
import silo.lang.compiler.Compiler;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

// TODO: Get rid of this class all together? Or, perhaps, add a bunch of the other forms as well.

public class ArrayGet implements Expression {

    public final Expression expression;
    public final Expression index;

    public static ArrayGet build(Node node) {
        Expression expression;
        Expression index;

        expression = Compiler.buildExpression(node.getFirstChild());
        index = Compiler.buildExpression(node.getSecondChild());

        return new ArrayGet(expression, index);
    }

    public ArrayGet(Expression expression, Expression index) {
        this.expression = expression;
        this.index = index;
    }

    public void emit(CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        expression.emit(context);
        Class arrayClass = frame.operandStack.peek();

        if(arrayClass.isArray()) {
            index.emit(context);
            Class indexClass = frame.operandStack.peek();

            if(indexClass.equals(Integer.TYPE)) {
                generator.arrayLoad(Type.getType(arrayClass.getComponentType()));

                frame.operandStack.pop();
                frame.operandStack.pop();
                frame.operandStack.pop();
            } else {
                throw new RuntimeException("");
            }
        } else {
            // TODO: Support Vars
            throw new RuntimeException("");
        }
    }
}
