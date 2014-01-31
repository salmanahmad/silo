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

import org.objectweb.asm.commons.GeneratorAdapter;
import org.apache.commons.lang3.StringUtils;

public class Package implements Expression {

    public final Node node;

    public Package(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Null.class;
    }

    public String packageName(Node node) {
        Vector<Symbol> list = Compiler.symbolList(node.getFirstChild());
        if(list != null) {
            return StringUtils.join(list, ".");
        }

        throw new RuntimeException("Invalid package declaration");
    }

    public void emitDeclaration(CompilationContext context) {
        // TODO: Refactor this code with emit below it... perhaps with an anonymous function
        // that calls emit vs emitDeclaration...

        if(node.getChildren().size() == 1) {
            context.currentNamespace().packageName = packageName(node);
        } else if(node.getChildren().size() == 2) {
            String name = context.currentNamespace().packageName;
            context.currentNamespace().packageName = packageName(node);

            Compiler.buildExpression(node.getSecondChild()).emitDeclaration(context);

            context.currentNamespace().packageName = name;
        } else {
            throw new RuntimeException("Invalid package declaration");
        }
    }

    public void emit(CompilationContext context) {
        // TODO: Refactor this code with emitDeclaration below it...

        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        if(node.getChildren().size() == 1) {
            context.currentNamespace().packageName = packageName(node);
        } else if(node.getChildren().size() == 2) {
            String name = context.currentNamespace().packageName;
            context.currentNamespace().packageName = packageName(node);

            Compiler.buildExpression(node.getSecondChild()).emit(context);

            context.currentNamespace().packageName = name;
        } else {
            throw new RuntimeException("Invalid package declaration");
        }

        generator.push((String)null);
        frame.operandStack.push(Null.class);
    }
}