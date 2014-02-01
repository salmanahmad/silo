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

public class LiteralArrayType implements Expression {

    public final Node node;

    public LiteralArrayType(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Class.class;
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        CompilationFrame frame = context.currentFrame();

        Class klass = Compiler.resolveType(node, context);
        if(klass == null) {
            throw new RuntimeException("Could not resolve array type: " + klass);
        }

        generator.visitLdcInsn(Type.getType(klass));
        frame.operandStack.push(Class.class);

        // TODO: This is not correct, right?
        // frame.operandStack.push(klass);
    }
}
