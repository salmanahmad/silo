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
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.Opcodes;

public class Finally implements Expression {

    Node node;

    public Finally(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Null.class;
    }

    public void validate() {
        if(node.getChildren().size() != 1) {
            throw new RuntimeException("Finally statements must only 1 argument");
        }
    }

    public Object scaffold(CompilationContext context) {
        validate();

        context.enterFinallyScaffold();
        Object scaffolded = Compiler.buildExpression(node.getFirstChild()).scaffold(context);
        context.exitFinallyScaffold();

        return new Node(node.getLabel(), new Node(null, scaffolded));
    }

    public void emit(CompilationContext context) {
        validate();

        Compiler.buildExpression(node.getFirstChild()).emit(context);

        context.currentFrame().generator.pop();
        context.currentFrame().generator.push((String)null);
        context.currentFrame().operandStack.pop();
        context.currentFrame().operandStack.push(Null.class);
    }
}
