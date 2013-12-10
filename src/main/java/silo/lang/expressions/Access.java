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


public class Access implements Expression {

    public final Expression receiver;
    public final Symbol name;

    public static Access build(Node node) {
        if(node.getChildren().size() != 2) {
            // TODO: Really? Do I want to do this? Being able to do ".(System out println)" could have its benefits...
            throw new RuntimeException("The '.' operator must have only 2 arguments");
        }

        if(!(node.getSecondChild() instanceof Symbol)) {
            throw new RuntimeException("The '.' operator must take a symbol to look up. It was provided: " + node.getSecondChild());
        } else {
            return new Access(Compiler.buildExpression(node.getFirstChild()), (Symbol)node.getSecondChild());
        }
    }

    public Access(Expression receiver, Symbol name) {
        this.receiver = receiver;
        this.name = name;
    }

    public void emit(CompilationContext context) {
        
    }
}
