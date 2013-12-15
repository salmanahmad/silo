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

public class Branch implements Expression {

    public final Expression condition;
    public final Expression trueBranch;
    public final Expression falseBranch;

    public static Branch build(Node node) {
        if(node.getChildren().size() > 3) {
            throw new RuntimeException("Branch must only have 3 arguments.");
        }

        return new Branch(
            Compiler.buildExpression(node.getChildren().get(0)),
            Compiler.buildExpression(node.getChildren().get(1)),
            Compiler.buildExpression(node.getChildren().get(2))
        );
    }

    public Branch(Expression condition, Expression trueBranch, Expression falseBranch) {
        if(trueBranch == null && falseBranch != null) {
            throw new RuntimeException("You cannot have a false branch without a true branch");
        }

        this.condition = condition;
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
    }

    public void emit(CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        // TODO: Make this var
        Class outputType = Object.class;

        Label falseLabel = generator.newLabel();
        Label endLabel = generator.newLabel();

        condition.emit(context);
        Class klass = context.currentFrame().operandStack.pop();

        if(klass.equals(Boolean.TYPE)) {
            generator.ifZCmp(GeneratorAdapter.EQ, falseLabel);
        } else if(klass.isPrimitive()) {
            // Implicitly will fall through to the trueLabel
            Compiler.pop(klass, generator);
        } else {
            generator.ifNull(falseLabel);
        }

        if(trueBranch != null) {
            trueBranch.emit(context);

            // TODO: Box into a var
            generator.box(Type.getType(frame.operandStack.pop()));
        } else {
            generator.push((String)null);
        }
        generator.goTo(endLabel);

        generator.mark(falseLabel);
        if(falseBranch != null) {
            falseBranch.emit(context);

            // TODO: Box into a var
            generator.box(Type.getType(frame.operandStack.pop()));
        } else {
            generator.push((String)null);
        }

        generator.mark(endLabel);

        frame.operandStack.push(outputType);
    }
}
