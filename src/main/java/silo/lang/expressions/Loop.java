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

public class Loop implements Expression {

    Node node;

    public Loop(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        // TODO: This should be a Var
        return Object.class;
    }

    public void emitDeclaration(CompilationContext context) {
        Expression code = Compiler.buildExpression(new Node(null, node.getChildren()));
        code.emitDeclaration(context);
    }

    public void emit(CompilationContext context) {
        Expression code = Compiler.buildExpression(new Node(null, node.getChildren()));

        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        Label startLabel = generator.newLabel();
        Label endLabel = generator.newLabel();

        frame.pushIterationFrame(startLabel, endLabel);

        generator.mark(startLabel);
        code.emit(context);

        frame.operandStack.pop();
        generator.pop();

        generator.goTo(startLabel);
        generator.mark(endLabel);

        frame.popIterationFrame();

        // TODO: This should be var. I am adding to the operandstack here because I do not
        // add to the operand stack in the branch (is this a typo, should be break?) expression since it would screw things up.
        frame.operandStack.push(Object.class);
    }
}
