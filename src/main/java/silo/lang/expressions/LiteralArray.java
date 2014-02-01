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

public class LiteralArray implements Expression {

    Node node;

    // TODO: Figure out a way to remove this or abstract them out...
    Object type;
    Vector<Expression> dimensions;

    public LiteralArray(Node node) {
        this.node = node;
    }

    public void process() {
        Vector children = node.getChildren();

        type = children.get(0);
        dimensions = new Vector<Expression>();

        for(int i = 1; i < children.size(); i++) {
            dimensions.add(Compiler.buildExpression(children.get(i)));
        }
    }

    public Class type(CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        int size = frame.operandStack.size();

        emit(context, false);

        if(frame.operandStack.size() - size != 1) {
            throw new RuntimeException("Error!");
        }

        return frame.operandStack.pop();
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        emit(context, true);
    }

    private void emit(CompilationContext context, boolean shouldEmit) {
        process();

        GeneratorAdapter generator = context.currentFrame().generator;
        CompilationFrame frame = context.currentFrame();

        int depth = this.dimensions.size();

        if(depth == 0) {
            throw new RuntimeException("An array literal must have at-least one dimension with a size. That size can be 0, if you want.");
        }

        Class klass = Compiler.resolveType(type, context);
        if(klass == null) {
            throw new RuntimeException("Could not resolve array type: " + type);
        }

        for(Expression expression : dimensions) {
            if(shouldEmit) {
                expression.emit(context);
            } else {
                frame.operandStack.push(expression.type(context));
            }

            Class operand = frame.operandStack.peek();
            if(!operand.equals(Integer.TYPE)) {
                // TODO: Support vars here as well as Integer (the boxed version of ints)
                throw new RuntimeException("Array dimensions must be integers. Provided: " + operand);
            }
        }

        for(Expression expression : dimensions) {
            if(shouldEmit) {
                generator.newArray(Type.getType(klass));
            }

            frame.operandStack.pop();

            klass = Array.newInstance(klass, 0).getClass();
        }

        frame.operandStack.push(klass);
    }
}
