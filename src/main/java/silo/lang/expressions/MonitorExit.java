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

public class MonitorExit implements Expression {

    Node node;

    public MonitorExit(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Null.class;
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        CompilationFrame frame = context.currentFrame();

        if(node.getChildren().size() != 1) {
            throw new RuntimeException("monitorexit must have 1 argument");
        }

        Object child = node.getChildren().get(0);
        Expression expression = Compiler.buildExpression(child);

        if(expression.type(context).isPrimitive()) {
            throw new RuntimeException("monitorexit cannot be applied to primitive values.");
        }

        expression.emit(context);

        generator.monitorExit();
        context.currentFrame().operandStack.pop();

        (new LiteralNull()).emit(context);
    }
}
