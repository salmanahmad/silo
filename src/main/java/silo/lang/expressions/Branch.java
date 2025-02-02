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

    Node node;

    // TODO: Remove these fields... and clean them up...
    Expression condition;
    Expression trueBranch;
    Expression falseBranch;

    public Branch(Node node) {
        this.node = node;
    }

    public void validate() {
        int size = node.getChildren().size();

        if(size == 1) {
            this.condition = Compiler.buildExpression(node.getChildren().get(0));
            this.trueBranch = null;
            this.falseBranch = null;
        } else if(size == 2) {
            this.condition = Compiler.buildExpression(node.getChildren().get(0));
            this.trueBranch = Compiler.buildExpression(node.getChildren().get(1));
            this.falseBranch = null;
        } else if(size == 3) {
            this.condition = Compiler.buildExpression(node.getChildren().get(0));
            this.trueBranch = Compiler.buildExpression(node.getChildren().get(1));
            this.falseBranch = Compiler.buildExpression(node.getChildren().get(2));
        } else {
            throw new RuntimeException("Invalid number of arguments to the branch form.");
        }

        if(trueBranch == null && falseBranch != null) {
            throw new RuntimeException("You cannot have a false branch without a true branch");
        }
    }

    public Class type(CompilationContext context) {
        validate();

        Class trueClass = null;
        Class falseClass = null;

        if(trueBranch != null) {
            trueClass = trueBranch.type(context);
        }

        if(falseBranch != null) {
            falseClass = falseBranch.type(context);
        }

        if(trueBranch != null && falseBranch != null) {
            if(trueClass.equals(falseClass)) {
                return trueClass;
            }
        }

        // TODO: This should be Var
        return Object.class;
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        validate();

        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        Label falseLabel = generator.newLabel();
        Label endLabel = generator.newLabel();

        Label falseEndLabel = generator.newLabel();
        Label trueEndLabel = generator.newLabel();

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

        Class outputType = type(context);

        if(trueBranch != null) {
            trueBranch.emit(context);
            Class operand = frame.operandStack.pop();
            if(operand.isPrimitive() && outputType.equals(Object.class)) {
                // TODO: This should be Var
                generator.box(Type.getType(operand));
            }
        } else {
            generator.push((String)null);
        }
        generator.goTo(endLabel);

        generator.mark(falseLabel);
        if(falseBranch != null) {
            falseBranch.emit(context);
            Class operand = frame.operandStack.pop();
            if(operand.isPrimitive() && outputType.equals(Object.class)) {
                // TODO: This should be Var
                generator.box(Type.getType(operand));
            }
        } else {
            generator.push((String)null);
        }
        generator.goTo(endLabel);

        generator.mark(endLabel);
        frame.operandStack.push(outputType);
    }
}
