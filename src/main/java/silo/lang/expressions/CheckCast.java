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

public class CheckCast implements Expression {

    Node node;

    public CheckCast(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        Object o = node.getSecondChild();
        Class klass = Compiler.resolveType(o, context);

        if(klass == null) {
            throw new RuntimeException("Could not resolve type: " + o);
        }

        return klass;
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        CompilationFrame frame = context.currentFrame();

        Class type = type(context);

        Expression e = Compiler.buildExpression(node.getFirstChild());
        e.emit(context);

        // TODO: Abstract this logic out when we support Vars and autoboxing
        if(type.isPrimitive()) {
            generator.cast(Type.getType(e.type(context)), Type.getType(type));
        } else {
            generator.checkCast(Type.getType(type));
        }
        frame.operandStack.pop();
        frame.operandStack.push(type);
    }
}
