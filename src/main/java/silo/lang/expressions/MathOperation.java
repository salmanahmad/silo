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

    public Class type(CompilationContext context) {
        return implicitConversion(this.e1.type(context), this.e2.type(context));
    }

    public void emit(CompilationContext context) {
        // TODO: Autoboxing and Var support...

        Class outputType = type(context);
        Class operand = null;

        this.e1.emit(context);
        operand = context.currentFrame().operandStack.peek();
        context.currentFrame().generator.cast(Type.getType(operand), Type.getType(outputType));

        this.e2.emit(context);
        operand = context.currentFrame().operandStack.peek();
        context.currentFrame().generator.cast(Type.getType(operand), Type.getType(outputType));

        context.currentFrame().generator.math(this.operation, Type.getType(outputType));

        context.currentFrame().operandStack.pop();
        context.currentFrame().operandStack.pop();
        context.currentFrame().operandStack.push(outputType);
    }
}
    