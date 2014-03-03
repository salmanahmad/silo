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
import silo.lang.compiler.Compiler;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

public class InstanceOf implements Expression {

    Node node;

    public InstanceOf(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Boolean.TYPE;
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        CompilationFrame frame = context.currentFrame();

        Object o = node.getSecondChild();
        Class type = Compiler.resolveType(o, context);

        if(type == null) {
            throw new RuntimeException("Could not resolve type: " + o);
        }

        Expression e = Compiler.buildExpression(node.getFirstChild());
        e.emit(context);

        generator.instanceOf(Type.getType(type));
        frame.operandStack.pop();
        frame.operandStack.push(Boolean.TYPE);
    }
}
