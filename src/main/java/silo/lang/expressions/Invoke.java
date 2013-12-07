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

import java.util.Vector;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.GeneratorAdapter;


import java.io.PrintStream;


public class Invoke implements Expression {

    public Expression name;
    public Vector<Expression> arguments;

    public Invoke(Expression name, Vector<Expression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public void emit(CompilationContext context, GeneratorAdapter generator) {


        generator.getStatic(Type.getType(System.class), "out", Type.getType(PrintStream.class));

        for(Expression e : arguments) {
            e.emit(context, generator);
        }

        generator.invokeVirtual(Type.getType(PrintStream.class), Method.getMethod("void println (int)"));

        for(Expression e : arguments) {
            context.operandStack.pop();
        }
    }
}
