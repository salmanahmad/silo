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

public class Catch implements Expression {

    Node node;

    public Catch(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Null.class;
    }

    public void validate() {
        if(node.getChildren().size() != 3) {
            throw new RuntimeException("Catch statement must have 3 arguments");
        }

        Object o = node.getChildren().get(1);
        if(o instanceof Node) {
            Node n = (Node)o;
            if(n.getLabel().equals(new Symbol(":"))) {
                if(n.getFirstChild() instanceof Symbol) {
                    return;
                }
            }
        }

        throw new RuntimeException("Catch statement must have a typed variable");
    }

    public Object scaffold(CompilationContext context) {
        validate();

        Symbol newVariableName = context.uniqueIdentifier("exception:variable");

        Symbol variableName = (Symbol)(((Node)node.getSecondChild()).getFirstChild());
        Object type = (Symbol)(((Node)node.getSecondChild()).getSecondChild());

        Vector oldChildren = node.getChildren();
        Vector children = new Vector();

        children.add(Compiler.buildExpression(oldChildren.get(0)).scaffold(context));
        children.add(new Node(new Symbol(":"), newVariableName, type));

        Object o = Compiler.buildExpression(oldChildren.get(2)).scaffold(context);
        if(o instanceof Node) {
            children.add(Node.replaceSymbol((Node)o, variableName, newVariableName));
        } else if(o.equals(variableName)) {
            children.add(newVariableName);
        } else {
            children.add(o);
        }

        return new Node(node.getLabel(), children);
    }

    public void emit(CompilationContext context) {
        validate();

        GeneratorAdapter generator = context.currentFrame().generator;

        Label startLabel = generator.newLabel();
        Label handlerLabel = generator.newLabel();
        Label endLabel = generator.newLabel();


        Symbol variableName = (Symbol)(((Node)node.getSecondChild()).getFirstChild());
        Object type = (Symbol)(((Node)node.getSecondChild()).getSecondChild());

        Class klass = Compiler.resolveType(type, context);
        if(klass == null) {
            throw new RuntimeException("Could not find type: " + klass);
        }

        int local = context.currentFrame().newLocal(variableName, klass);

        generator.mark(startLabel);
        Compiler.buildExpression(node.getFirstChild()).emit(context);
        generator.pop();
        context.currentFrame().operandStack.pop();
        generator.goTo(endLabel);

        generator.mark(handlerLabel);
        generator.catchException(startLabel, handlerLabel, Type.getType(klass));
        generator.visitFrame(Opcodes.F_NEW, 0, null, 1, new Object[] {Type.getType(klass).getInternalName()});
        generator.visitVarInsn(Opcodes.ASTORE, local);
        Compiler.buildExpression(node.getThirdChild()).emit(context);
        generator.pop();
        context.currentFrame().operandStack.pop();
        generator.mark(endLabel);

        generator.push((String)null);
        context.currentFrame().operandStack.push(Null.class);
    }
}
