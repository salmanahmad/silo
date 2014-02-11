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
import org.objectweb.asm.Type;

public class UniqueSymbol implements Expression {

    Node node;

    public UniqueSymbol(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Symbol.class;
    }

    public Object scaffold(CompilationContext context) {
        if(node.getChildren() != null && node.getChildren().size() != 0) {
            throw new RuntimeException("uniquesymbol cannot have any arguments");
        }

        return node;
    }

    public void emit(CompilationContext context) {
        context.currentFrame().generator.newInstance(Type.getType(Symbol.class));
        context.currentFrame().generator.dup();
        context.currentFrame().generator.push(context.uniqueIdentifier("unique:symbol").toString());
        context.currentFrame().generator.invokeConstructor(Type.getType(Symbol.class), org.objectweb.asm.commons.Method.getMethod("void <init> (String)"));

        context.currentFrame().operandStack.push(Symbol.class);
    }
}
