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

public class ArraySet implements Expression {

    public final Expression expression;
    public final Expression index;
    public final Expression value;

    public static ArraySet build(Node node) {
        Expression expression = Compiler.buildExpression(node.getChild(0));
        Expression index = Compiler.buildExpression(node.getChild(1));;
        Expression value = Compiler.buildExpression(node.getChild(2));;

        return new ArraySet(expression, index, value);
    }

    public ArraySet(Expression expression, Expression index, Expression value) {
        this.expression = expression;
        this.index = index;
        this.value = value;
    }

    public Class type(CompilationContext context) {
        return null;
    }

    public void emit(CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        value.emit(context);
        Class valueClass = frame.operandStack.peek();
        Compiler.dup(valueClass, generator);
        frame.operandStack.push(valueClass);

        expression.emit(context);
        Class arrayClass = frame.operandStack.peek();

        generator.swap(Type.getType(valueClass), Type.getType(arrayClass));

        if(arrayClass.isArray()) {
            if(!arrayClass.getComponentType().isAssignableFrom(valueClass)) {
                throw new RuntimeException("Attempting to assign value into array of invalid types");
            }

            index.emit(context);
            Class indexClass = frame.operandStack.peek();

            if(indexClass.equals(Integer.TYPE)) {
                generator.swap(Type.getType(valueClass), Type.getType(indexClass));
                generator.arrayStore(Type.getType(arrayClass.getComponentType()));

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
