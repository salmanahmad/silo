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
import org.objectweb.asm.commons.Method;

import org.apache.commons.lang3.ClassUtils;

public class Throw implements Expression {

    Node node;

    public Throw(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
/*
        if(this.node.getChildren().size() == 1) {
            Expression e = Compiler.buildExpression(this.node.getChildren().get(0));
            Class klass = e.type(context);

            if(Throwable.class.isAssignableFrom(klass)) {
                return klass;
            }
        }

        return String.class;
*/

        return Null.class;
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        CompilationFrame frame = context.currentFrame();

        if(node.getChildren().size() > 1) {
            throw new RuntimeException("throw cannot have more than 1 argument");
        }

        Object child = null;
        if(node.getChildren().size() == 0) {
            child = "";
        } else {
            child = node.getChildren().get(0);
        }

        Compiler.buildExpression(child).emit(context);

        Class klass = context.currentFrame().operandStack.peek();
        if(klass.isPrimitive()) {
            generator.valueOf(Type.getType(klass));

            klass = ClassUtils.primitiveToWrapper(klass);

            context.currentFrame().operandStack.pop();
            context.currentFrame().operandStack.push(klass);
        }

        if(!Throwable.class.isAssignableFrom(klass)) {
            generator.invokeVirtual(Type.getType(Object.class), Method.getMethod("String toString()"));

            klass = String.class;

            context.currentFrame().operandStack.pop();
            context.currentFrame().operandStack.push(klass);

            generator.newInstance(Type.getType(RuntimeException.class));
            generator.dupX1();
            generator.dupX1();
            generator.pop();

            generator.invokeConstructor(Type.getType(RuntimeException.class), Method.getMethod("void <init> (String)"));
        }


        generator.throwException();
        context.currentFrame().operandStack.pop();

        (new LiteralNull()).emit(context);
    }
}
