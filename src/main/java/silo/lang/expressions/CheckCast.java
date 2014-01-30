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
        Object o = node.getFirstChild();
        Class klass = Compiler.resolveType(o, context);

        if(klass == null) {
            throw new RuntimeException("Could not resolve type: " + o);
        }

        return klass;
    }

    public void emitDeclaration(CompilationContext context) {
        for(Object child : node.getChildren()) {
            Compiler.buildExpression(child).emitDeclaration(context);
        }
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        CompilationFrame frame = context.currentFrame();

        Class type = type(context);

        Expression e = Compiler.buildExpression(node.getSecondChild());
        e.emit(context);

        generator.checkCast(Type.getType(type));
        frame.operandStack.pop();
        frame.operandStack.push(type);
    }
}
