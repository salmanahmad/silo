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
import org.objectweb.asm.commons.GeneratorAdapter;

public class Return implements Expression {

    public final Expression value;

    public static Return build(Node node) {
        return new Return(Block.build(node));
    }

    public Return(Expression value) {
        this.value = value;
    }

    public void emit(CompilationContext context) {
        if(value != null) {
            value.emit(context);
        }

        // TODO: Branch statements needs to be careful to always return a Var if the types do not agree with one another...

        Class outputClass = context.currentFrame().outputClass;
        GeneratorAdapter g = context.currentFrame().generator;

        if(context.currentFrame().operandStack.size() == 0) {
            if(outputClass.equals(Object.class)) {
                // TODO: AKA Var.class. Change this if statement for Vars
                g.push((String)null);
                g.returnValue();
            } else if(outputClass.equals(Void.TYPE)) {
                g.push((String)null);
                g.returnValue();
            } else {
                throw new RuntimeException("Expecting a return type but there isn't any.");
            }
        } else if(context.currentFrame().operandStack.size() == 1) {
            Class operand = context.currentFrame().operandStack.pop();

            if(outputClass.equals(Object.class)) {
                // TODO: AKA Var.class. Change this if statement for Vars
                g.valueOf(Type.getType(operand));
                g.returnValue();
            } else if(outputClass.equals(Void.TYPE)) {
                Compiler.pop(operand, g);
                g.push((String)null);
                g.returnValue();
            } else {
                if(outputClass.isAssignableFrom(operand)) {
                    g.returnValue();
                } else {
                    throw new RuntimeException("Invalid return type from function.");
                }
            }
        } else {
            throw new RuntimeException("Too many things on the operand stack when returning from a function.");
        }
    }
}