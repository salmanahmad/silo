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

import java.util.HashMap;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

// TODO: Redo this as a macro...

public class LogicalOperation implements Expression {

    public static boolean accepts(Object value) {
        if(value instanceof Symbol) {
            if(value.equals(new Symbol("&&")) || value.equals(new Symbol("and"))) {
                return true;
            }

            if(value.equals(new Symbol("||")) || value.equals(new Symbol("or"))) {
                return true;
            }

            if(value.equals(new Symbol("!")) || value.equals(new Symbol("not"))) {
                return true;
            }
        }

        return false;
    }

    public static Expression build(Node node) {
        Symbol value = (Symbol)node.getLabel();

        if(value.equals(new Symbol("&&")) || value.equals(new Symbol("and"))) {
            if(node.getChildren().size() != 2) {
                throw new RuntimeException("Invalid logical operation");
            }

            Object first = node.getFirstChild();
            Object second = node.getSecondChild();

            Node expression = new Node(new Symbol("branch"), first,
                new Node(new Symbol("branch"), second,
                    new Boolean(true),
                    new Boolean(false)
                ),
                new Boolean(false)
            );

            return Compiler.buildExpression(expression);
        }

        if(value.equals(new Symbol("||")) || value.equals(new Symbol("or"))) {
            if(node.getChildren().size() != 2) {
                throw new RuntimeException("Invalid logical operation");
            }

            Object first = node.getFirstChild();
            Object second = node.getSecondChild();

            Node expression = new Node(new Symbol("branch"), first,
                new Boolean(true),
                new Node(new Symbol("branch"), second,
                    new Boolean(true),
                    new Boolean(false)
                )
            );

            return Compiler.buildExpression(expression);
        }

        if(value.equals(new Symbol("!")) || value.equals(new Symbol("not"))) {
            if(node.getChildren().size() != 1) {
                throw new RuntimeException("Invalid logical operation");
            }

            Object first = node.getFirstChild();

            Node expression = new Node(new Symbol("branch"), first,
                new Boolean(false),
                new Boolean(true)
            );

            return Compiler.buildExpression(expression);

        }

        throw new RuntimeException("Invalid logical operation");
    }

    public Class type(CompilationContext context) {
        throw new RuntimeException("LogicalOperation should not ever be called...");
    }

    public Object scaffold(CompilationContext context) {
        throw new RuntimeException("LogicalOperation should not ever be called...");
    }

    public void emit(CompilationContext context) {}
}
