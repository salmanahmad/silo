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

public class MathOperation implements Expression {

    public final Expression e1;
    public final Expression e2;
    public int operation;

    static HashMap<Symbol, Integer> opcodes = new HashMap<Symbol, Integer>();

    static {
        // TODO: Should I put the follow lines as well?
        //opcodes.put(new Symbol("add"), GeneratorAdapter.ADD);

        opcodes.put(new Symbol("+"), GeneratorAdapter.ADD);
        opcodes.put(new Symbol("-"), GeneratorAdapter.SUB);
        opcodes.put(new Symbol("*"), GeneratorAdapter.MUL);
        opcodes.put(new Symbol("/"), GeneratorAdapter.DIV);
        opcodes.put(new Symbol("%"), GeneratorAdapter.REM);
    }

    public static boolean accepts(Object value) {
        if(value instanceof Symbol) {
            Symbol symbol = (Symbol)value;
            return opcodes.containsKey(symbol);
        }

        return false;
    }

    public static MathOperation build(Node node) {

        if(node.getLabel() instanceof Symbol) {
            Symbol symbol = (Symbol)node.getLabel();
            Integer opcode = opcodes.get(symbol);

            if(opcode != null) {
                if(node.getChildren().size() != 2) {
                    throw new RuntimeException("Binary operation '" + symbol + "' must have 2 operands.");
                }

                return new MathOperation(
                    Compiler.buildExpression(node.getChildren().get(0)),
                    Compiler.buildExpression(node.getChildren().get(1)),
                    opcode.intValue());
            }
        }

        throw new RuntimeException("Invalid math operation.");
    }

    public MathOperation(Expression e1, Expression e2, int operation) {
        this.e1 = e1;
        this.e2 = e2;
        this.operation = operation;
    }

    public static Class implicitConversion(Class klass1, Class klass2) {
        HashMap<Class, Integer> supportedTypes = new HashMap<Class, Integer>();
        supportedTypes.put(Byte.TYPE, 0);
        supportedTypes.put(Short.TYPE, 1);
        supportedTypes.put(Character.TYPE, 2);
        supportedTypes.put(Integer.TYPE, 3);
        supportedTypes.put(Long.TYPE, 4);
        supportedTypes.put(Float.TYPE, 5);
        supportedTypes.put(Double.TYPE, 6);

        if(supportedTypes.containsKey(klass1) && supportedTypes.containsKey(klass2)) {
            int index1 = supportedTypes.get(klass1);
            int index2 = supportedTypes.get(klass2);

            if(index1 > index2) {
                return klass1;
            } else {
                return klass2;
            }
        }

        return null;
    }

    public void emit(CompilationContext context) {
        Class operand1 = null;
        Class operand2 = null;

        this.e1.emit(context);
        operand1 = context.currentFrame().operandStack.peek();

        this.e2.emit(context);
        operand2 = context.currentFrame().operandStack.peek();

        Class outputType = implicitConversion(operand1, operand2);
        if(outputType == null) {
            throw new RuntimeException("Invalid math operation. Cannot perform operation on a " + operand1 + " and a " + operand2);
        }

        if(!operand1.equals(operand2)) {
            // TODO: Optimize this. The issue I have right now is that "emit" adds all of the instructions
            // to a MethodVisitor rather than returning an InsnList so I cannot tell what the type is ahead of time.
            // Once I figure out how to unify those APIs I should optimze this. Or, replace them with a bunch of Static calls
            // to runtime methods that handle the math for me if unifying the APIs ais not worth it...
            context.currentFrame().generator.cast(Type.getType(operand2), Type.getType(outputType));
            context.currentFrame().generator.swap(Type.getType(operand1), Type.getType(outputType));
            context.currentFrame().generator.cast(Type.getType(operand1), Type.getType(outputType));
            context.currentFrame().generator.swap(Type.getType(outputType), Type.getType(outputType));
        }

        context.currentFrame().generator.math(this.operation, Type.getType(outputType));

        context.currentFrame().operandStack.pop();
        context.currentFrame().operandStack.pop();

        context.currentFrame().operandStack.push(outputType);
    }
}
    