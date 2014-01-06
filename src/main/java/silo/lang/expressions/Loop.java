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

    public final Expression code;

    public static Loop build(Node node) {
        return new Loop(
            Block.build(node)
        );
    }

    public Loop(Expression code) {
        this.code = code;
    }

    public void emit(CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        Label startLabel = generator.newLabel();
        Label endLabel = generator.newLabel();

        frame.pushIterationFrame(startLabel, endLabel);

        generator.mark(startLabel);
        this.code.emit(context);

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
