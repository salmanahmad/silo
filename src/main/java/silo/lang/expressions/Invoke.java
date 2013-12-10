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
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.GeneratorAdapter;


import java.io.PrintStream;


public class Invoke implements Expression {

    public final Expression receiver;
    public final Symbol method;
    public final Vector<Expression> arguments;

    public static Invoke build(Node node) {
        Expression receiver = null;
        Symbol method = null;
        Vector<Expression> arguments = new Vector();

        Object label = node.getLabel();
        if(label != null) {
            if(label instanceof Symbol) {
                method = (Symbol)label;
            } else if(label instanceof Node) {
                Node n = (Node)label;

                if(n.getLabel().equals(new Symbol("."))) {
                    if(!(n.getSecondChild() instanceof Symbol)) {
                        throw new RuntimeException("Calling a function must be symbol");
                    }

                    receiver = Compiler.buildExpression(n.getFirstChild());
                    method = (Symbol)n.getSecondChild();
                } else {
                    receiver = Compiler.buildExpression(n);
                }
            } else {
                receiver = Compiler.buildExpression(label);
            }
        }

        for(Object child : node.getChildren()) {
            arguments.add(Compiler.buildExpression(child));
        }

        return new Invoke(receiver, method, arguments);
    }

    public Invoke(Expression receiver, Symbol method, Vector<Expression> arguments) {
        if(receiver == null && method == null) {
            throw new RuntimeException("An invocation needs to have either a receiver or a method.");
        }

        this.receiver = receiver;
        this.method = method;
        this.arguments = arguments;
    }

    public void emit(CompilationContext context) {

        GeneratorAdapter generator = context.currentFrame().generator;

        

        generator.getStatic(Type.getType(System.class), "out", Type.getType(PrintStream.class));

        for(Expression e : arguments) {
            e.emit(context);
        }

        generator.invokeVirtual(Type.getType(PrintStream.class), Method.getMethod("void println (int)"));

        for(Expression e : arguments) {
            context.currentFrame().operandStack.pop();
        }
    }
}
