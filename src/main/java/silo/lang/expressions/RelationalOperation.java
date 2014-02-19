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

public class RelationalOperation implements Expression {

    Node node;

    static HashMap<Symbol, Integer> opcodes = new HashMap<Symbol, Integer>();
    static {
        opcodes.put(new Symbol("=="), GeneratorAdapter.EQ);
        opcodes.put(new Symbol("!="), GeneratorAdapter.NE);
        opcodes.put(new Symbol(">="), GeneratorAdapter.GE);
        opcodes.put(new Symbol(">"), GeneratorAdapter.GT);
        opcodes.put(new Symbol("<="), GeneratorAdapter.LE);
        opcodes.put(new Symbol("<"), GeneratorAdapter.LT);
    }

    public static boolean accepts(Object value) {
        if(value instanceof Symbol) {
            Symbol symbol = (Symbol)value;
            return opcodes.containsKey(symbol);
        }

        return false;
    }

    public RelationalOperation(Node node) {
        this.node = node;
    }

    public void validate() {
        if(node.getLabel() instanceof Symbol) {
            Symbol symbol = (Symbol)node.getLabel();
            Integer opcode = opcodes.get(symbol);

            if(opcode != null) {
                if(node.getChildren().size() != 2) {
                    throw new RuntimeException("Binary operation '" + symbol + "' must have 2 operands.");
                }

                return;
            }
        }

        throw new RuntimeException("Invalid relational operation.");
    }

    public Class type(CompilationContext context) {
        return Boolean.TYPE;
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        // TODO: Add support for non-primitive types.
        // TODO: Autoboxing and Var support...
        validate();

        Expression e1 = Compiler.buildExpression(node.getChildren().get(0));
        Expression e2 = Compiler.buildExpression(node.getChildren().get(1));
        int operation = opcodes.get((Symbol)node.getLabel()).intValue();

        Class operand = null;
        Class operand1 = e1.type(context);
        Class operand2 = e2.type(context);

        Class outputType = MathOperation.implicitConversion(operand1, operand2);

        if(outputType == null) {
            if(operand1.equals(Boolean.TYPE) && operand2.equals(Boolean.TYPE)) {
                // Boolean Comparison
                e1.emit(context);
                e2.emit(context);

                if(operation == GeneratorAdapter.EQ || operation == GeneratorAdapter.NE) {
                    Label trueLabel = context.currentFrame().generator.newLabel();
                    Label endLabel = context.currentFrame().generator.newLabel();

                    context.currentFrame().generator.ifCmp(Type.getType(Integer.TYPE), operation, trueLabel);
                    context.currentFrame().generator.push(false);
                    context.currentFrame().generator.goTo(endLabel);
                    context.currentFrame().generator.mark(trueLabel);
                    context.currentFrame().generator.push(true);
                    context.currentFrame().generator.mark(endLabel);
                } else {
                    throw new RuntimeException("Invalid relational operation between two booleans");
                }
            } else if(operand1.isPrimitive() || operand2.isPrimitive()) {
                // Illegal Case
                throw new RuntimeException("Invalid relational operation between " + operand1 + " and " + operand2);
            } else {
                // Object Equality Comparison
                e1.emit(context);
                e2.emit(context);

                if(operation == GeneratorAdapter.EQ) {
                    context.currentFrame().generator.invokeStatic(Type.getType(Helper.class), Method.getMethod("boolean areObjectsEqual(Object, Object)"));
                } else if(operation == GeneratorAdapter.NE) {
                    context.currentFrame().generator.invokeStatic(Type.getType(Helper.class), Method.getMethod("boolean areObjectsNotEqual(Object, Object)"));
                } else {
                    // Object Relational Comparison

                    if(operand1.equals(operand2) && (Comparable.class.isAssignableFrom(operand1))) {
                        if(operation == GeneratorAdapter.LT) {
                            context.currentFrame().generator.invokeStatic(Type.getType(Helper.class), Method.getMethod("boolean compareToLessThan(Object, Object)"));
                        } else if(operation == GeneratorAdapter.LE) {
                            context.currentFrame().generator.invokeStatic(Type.getType(Helper.class), Method.getMethod("boolean compareToLessThanEqual(Object, Object)"));
                        } else if(operation == GeneratorAdapter.GT) {
                            context.currentFrame().generator.invokeStatic(Type.getType(Helper.class), Method.getMethod("boolean compareToGreaterThan(Object, Object)"));
                        } else if(operation == GeneratorAdapter.GE) {
                            context.currentFrame().generator.invokeStatic(Type.getType(Helper.class), Method.getMethod("boolean compareToGreaterThanEqual(Object, Object)"));
                        } else {
                            throw new RuntimeException("Invalid relational operation between " + operand1 + " and " + operand2);
                        }
                    } else {
                        throw new RuntimeException("Invalid relational operation between " + operand1 + " and " + operand2);
                    }
                }
            }
        } else {
            // Normal Relational Operation
            e1.emit(context);
            operand = context.currentFrame().operandStack.peek();
            context.currentFrame().generator.cast(Type.getType(operand), Type.getType(outputType));

            e2.emit(context);
            operand = context.currentFrame().operandStack.peek();
            context.currentFrame().generator.cast(Type.getType(operand), Type.getType(outputType));

            Label trueLabel = context.currentFrame().generator.newLabel();
            Label endLabel = context.currentFrame().generator.newLabel();

            context.currentFrame().generator.ifCmp(Type.getType(outputType), operation, trueLabel);
            context.currentFrame().generator.push(false);
            context.currentFrame().generator.goTo(endLabel);
            context.currentFrame().generator.mark(trueLabel);
            context.currentFrame().generator.push(true);
            context.currentFrame().generator.mark(endLabel);
        }

        context.currentFrame().operandStack.pop();
        context.currentFrame().operandStack.pop();
        context.currentFrame().operandStack.push(Boolean.TYPE);
    }
}
    