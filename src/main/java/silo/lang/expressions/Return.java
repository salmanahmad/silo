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

    public final Node node;
    public final boolean explicit;

    public Return(Node node) {
        this(node, true);
    }

    public Return(Node node, boolean explicit) {
        this.node = node;
        this.explicit = explicit;
    }

    public static boolean isBoxClass(Class klass) {
        // TODO: Include var in this list...
        return (
            klass.equals(Object.class) ||
            klass.equals(Boolean.class) ||
            klass.equals(Character.class) ||
            klass.equals(Byte.class) ||
            klass.equals(Short.class) ||
            klass.equals(Integer.class) ||
            klass.equals(Long.class) ||
            klass.equals(Float.class) ||
            klass.equals(Double.class)
        );
    }

    public Class type(CompilationContext context) {
        // TODO: This should be var.
        return Object.class;
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        if(!context.currentFrame().finallyClauses.empty()) {
            for(int i = context.currentFrame().finallyClauses.size() - 1; i >= 0; i--) {
                Object clause = context.currentFrame().finallyClauses.get(i);
                Compiler.buildExpression(clause).emit(context);

                context.currentFrame().generator.pop();
                context.currentFrame().operandStack.pop();
            }
        }

        Expression value = null;

        if(node != null) {
            value = Compiler.buildExpression(new Node(null, node.getChildren()));
        }

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
                if(explicit) {
                    throw new RuntimeException("Expecting a return type but there isn't any.");
                }
            }
        } else if(context.currentFrame().operandStack.size() == 1) {
            // This is peek instead of pop because the compiler may have this after it even though
            // the JVM will not allow it. Will this cause errors with the JVM verifier?
            Class operand = context.currentFrame().operandStack.peek();

            if(outputClass.equals(Object.class)) {
                // TODO: AKA Var.class. Change this if statement for Vars
                g.valueOf(Type.getType(operand));
                g.returnValue();
            } else if(outputClass.equals(Void.TYPE)) {
                // TODO: This seems wrong. There will never be a Void function. At the veryleast it will be returning a "Var". Right now, a "var" means "object" but in either case, shouldn't I just return the operand straight out? Perhaps I first wrap it accordingly (into a var, or as a primitive). In particular, If I do not specify an output type, I think that it should just return the operand, not force return null...
                Compiler.pop(operand, g);
                g.push((String)null);
                g.returnValue();
            } else {
                if(Compiler.isValidAssignment(outputClass, operand)) {
                    g.returnValue();
                } else if(outputClass.isPrimitive() && isBoxClass(operand)) {
                    // TODO: This statement above should be: "equals object OR var"
                    g.unbox(Type.getType(outputClass));
                    g.returnValue();
                } else {
                    // TODO: Implicit conversion. Aka returning float form a double, etc. Not sure I want to support that yet.
                    // TODO: At the very least, do I want support inserting a CHECKCAST if operand is a Var?
                    throw new RuntimeException("Invalid return type from function. Expected: " + outputClass + ". Provided: " + operand);
                }
            }
        } else {
            throw new RuntimeException("Too many things on the operand stack when returning from a function." + context.currentFrame().operandStack);
        }
    }
}
