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
import java.util.HashMap;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

public class Pipe implements Expression {

    public static Expression build(Node node) {
        Symbol value = (Symbol)node.getLabel();

        if(value.equals(new Symbol("|"))) {
            if(node.getChildren().size() == 2) {
                Object first = node.getFirstChild();
                Object second = node.getSecondChild();

                if(second instanceof Node) {
                    Node source = (Node)second;

                    Vector children = source.getChildren();
                    children.insertElementAt(first, 0);

                    Node target = Node.withMeta(source.getMeta(), source.getLabel());
                    target.addChildren(children);
                    return Compiler.buildExpression(target);
                }
            }
        }

        throw new RuntimeException("Invalid pipe operation");
    }

    public Class type(CompilationContext context) {
        throw new RuntimeException("Pipe should not ever be called...");
    }

    public Object scaffold(CompilationContext context) {
        throw new RuntimeException("Pipe should not ever be called...");
    }

    public void emit(CompilationContext context) {
        throw new RuntimeException("Pipe should not ever be called...");
    }
}
