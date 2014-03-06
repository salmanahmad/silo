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

public class Try implements Expression {

    Node node;

    public Try(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Null.class;
    }

    public Object scaffold(CompilationContext context) {
        Vector oldChildren = node.getChildren();
        Vector children = new Vector();

        if(oldChildren.size() < 3) {
            throw new RuntimeException("A try statement must have at-least 3 arguments.");
        }

        for(int i = 0; i < oldChildren.size(); i++) {
            Object child = oldChildren.get(i);
            if(i == 0) {
                // First - this is the try block
                children.add(Compiler.buildExpression(child).scaffold(context));
            } else if(i == oldChildren.size() - 1) {
                // Last - must be finally
                // This is really weird control flow. Basically, if the "finally" symbol is not found then "i"
                // gets increments twice every iteration by the catch logic. Thus, the loop goes around and lands
                // back here if a finally was found.
                children.add(Compiler.buildExpression(child).scaffold(context));
            } else {
                // Not last and not first - must be catch / block
                if(child instanceof Node) {
                    Node n = (Node)child;

                    Symbol newVariableName = context.uniqueIdentifier("exception:variable");
                    Symbol variableName = null;
                    Object variableType = null;

                    try {
                         variableName = (Symbol)((Node)n.getFirstChild()).getFirstChild();
                         variableType = ((Node)n.getFirstChild()).getSecondChild();
                    } catch(ClassCastException e) {
                        throw new RuntimeException("Invalid catch statement. Variable needs to be a symbol.");
                    }

                    children.add(new Node(new Symbol(":"), newVariableName, variableType));

                    i++;
                    Object code = null;

                    try {
                        code = oldChildren.get(i);
                    } catch(java.lang.IndexOutOfBoundsException e) {
                        throw new RuntimeException("Missing catch code block.");
                    }

                    code = Compiler.buildExpression(code).scaffold(context);
                    if(code instanceof Node) {
                        children.add(Node.replaceSymbol((Node)code, variableName, newVariableName));
                    } else if(code.equals(variableName)) {
                        children.add(newVariableName);
                    } else {
                        children.add(code);
                    }
                } else if(child.equals(new Symbol("finally"))) {
                    // Ignore the finally symbol. The block is coming next.

                    if(i != oldChildren.size() - 2) {
                        throw new RuntimeException("Invalid finally specifier in try statement.");
                    }

                    children.add(child);
                } else {
                    throw new RuntimeException("Invalid catch statement");
                }
            }
        }

        return new Node(node.getLabel(), children);
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;

        Label tryStartLabel = generator.newLabel();
        Label tryEndLabel = generator.newLabel();
        Label finallyLabel = generator.newLabel();
        Label doneLabel = generator.newLabel();

        Object tryBlock = node.getFirstChild();
        Object finallyBlock = null;

        if(node.getChildren().get(node.getChildren().size() - 2).equals(new Symbol("finally"))) {
            finallyBlock = node.getChildren().get(node.getChildren().size() - 1);
        }

        generator.mark(tryStartLabel);
        if(finallyBlock != null) { context.currentFrame().finallyClauses.push(finallyBlock); }
        Compiler.buildExpression(tryBlock).emit(context);
        generator.pop();
        context.currentFrame().operandStack.pop();
        if(finallyBlock != null) { context.currentFrame().finallyClauses.pop(); }
        generator.mark(tryEndLabel);

        if(finallyBlock != null) {
            Compiler.buildExpression(finallyBlock).emit(context);
            generator.pop();
            context.currentFrame().operandStack.pop();
        }
        generator.goTo(doneLabel);

        for(int i = 1; i < node.getChildren().size() - 1; i++) {
            Object child = node.getChildren().get(i);

            if((i == node.getChildren().size() - 2) && child.equals(new Symbol("finally"))) {
                break;
            }

            Label catchStartLabel = generator.newLabel();
            Label catchEndLabel = generator.newLabel();

            // TODO: How do I abstract the need for constant casting for Silo using macros?
            Symbol variableName = (Symbol)((Node)child).getFirstChild();
            Object variableType = ((Node)child).getSecondChild();

            Class klass = Compiler.resolveType(variableType, context);
            if(klass == null) {
                throw new RuntimeException("Could not find type: " + variableType);
            }

            // TODO: Do I need to set this local during scaffold() or inside type()?
            int local = context.currentFrame().newLocal(variableName, klass);

            i++;
            Object code = node.getChildren().get(i);

            generator.catchException(tryStartLabel, tryEndLabel, Type.getType(klass));

            generator.mark(catchStartLabel);
            if(finallyBlock != null) { context.currentFrame().finallyClauses.push(finallyBlock); }
            generator.visitFrame(Opcodes.F_NEW, 0, null, 1, new Object[] {Type.getType(klass).getInternalName()});
            generator.visitVarInsn(Opcodes.ASTORE, local);
            Compiler.buildExpression(code).emit(context);
            generator.pop();
            context.currentFrame().operandStack.pop();
            if(finallyBlock != null) { context.currentFrame().finallyClauses.pop(); }
            generator.mark(catchEndLabel);
            if(finallyBlock != null) { generator.visitTryCatchBlock(catchStartLabel, catchEndLabel, finallyLabel, null); }

            if(finallyBlock != null) {
                Compiler.buildExpression(finallyBlock).emit(context);
                generator.pop();
                context.currentFrame().operandStack.pop();
            }
            generator.goTo(doneLabel);
        }

        if(finallyBlock != null) {
            generator.visitTryCatchBlock(tryStartLabel, tryEndLabel, finallyLabel, null);
            generator.mark(finallyLabel);
            generator.visitFrame(Opcodes.F_NEW, 0, null, 1, new Object[] {Type.getType(Throwable.class).getInternalName()});
            context.currentFrame().operandStack.push(Throwable.class);
            Compiler.buildExpression(finallyBlock).emit(context);
            generator.pop();
            context.currentFrame().operandStack.pop();

            generator.throwException();
            context.currentFrame().operandStack.pop();
        }

        generator.mark(doneLabel);
        generator.push((String)null);
        context.currentFrame().operandStack.push(Null.class);
    }
}
