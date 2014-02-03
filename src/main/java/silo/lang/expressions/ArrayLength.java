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

import java.util.Vector;
import java.lang.reflect.Array;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

public class ArrayLength implements Expression {

    Node node;

    public ArrayLength(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Integer.TYPE;
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        CompilationFrame frame = context.currentFrame();

        if(node.getChildren().size() != 1) {
            throw new RuntimeException("arraylength must have only 1 argument");
        }

        Object child = node.getChildren().get(0);
        Compiler.buildExpression(child).emit(context);

        Class klass = context.currentFrame().operandStack.peek();

        if(!klass.isArray()) {
            // TODO: Handle Object and Vars
            throw new RuntimeException("ArrayLength is applied to a non-array type.");
        }

        generator.arrayLength();
        frame.operandStack.pop();
        frame.operandStack.push(Integer.TYPE);
    }
}
