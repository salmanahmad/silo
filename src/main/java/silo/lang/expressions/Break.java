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

import java.util.Vector;

import org.objectweb.asm.Type;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.MethodNode;

public class Break implements Expression {

    Node node;

    public Break(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        // TODO: This should be var.
        return Object.class;
    }

    public Object scaffold(CompilationContext context) {
        // TODO: This line appeared in the old version of emitDeclarations and was present
        // in many other exprerssions, like return where I wrap the children in a block. Do I
        // need to wrap the children in a block in scaffold?
        //Expression value = Compiler.buildExpression(new Node(null, node.getChildren()));

        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        if(!context.currentFrame().finallyClauses.empty()) {
            for(int i = context.currentFrame().finallyClauses.size() - 1; i >= 0; i--) {
                Object clause = context.currentFrame().finallyClauses.get(i);
                Compiler.buildExpression(clause).emit(context);

                context.currentFrame().generator.pop();
                context.currentFrame().operandStack.pop();
            }
        }

        Expression value = Compiler.buildExpression(new Node(null, node.getChildren()));

        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        value.emit(context);

        // Note: I am explicitly not pop or pushing anything onto the operandStack because it is
        // taken care of in Loop. I am leaving the stack untouched for the sake of downstream isntructions
        // that may want to read the type of the operand. Basically, as far as the compiler is concerned,
        // the branch instruction does not exist.

        // Note: Block handle pushing a null if nothing was added so that it works nicely here.

        Class operand = frame.operandStack.peek();

        // TODO: Update this for Var
        if(!operand.equals(Object.class)) {
            if(operand.isPrimitive()) {
                // TODO: Update this to use the var primitive wrappers.
                generator.box(Type.getType(operand));
            } else {
                // TODO: Update this to use the var non-primitive wrappers
            }
        }

        generator.goTo(frame.iterationFrameEndLabels.peek());
    }
}
