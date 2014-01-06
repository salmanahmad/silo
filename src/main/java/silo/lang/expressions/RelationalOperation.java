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

    public final Expression e1;
    public final Expression e2;
    public int operation;

    static HashMap<Symbol, Integer> opcodes = new HashMap<Symbol, Integer>();

    static {
        // TODO: Should I put the follow lines as well?
        //opcodes.put(new Symbol("equals"), GeneratorAdapter.EQ);

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

    public static RelationalOperation build(Node node) {

        if(node.getLabel() instanceof Symbol) {
            Symbol symbol = (Symbol)node.getLabel();
            Integer opcode = opcodes.get(symbol);

            if(opcode != null) {
                if(node.getChildren().size() != 2) {
                    throw new RuntimeException("Binary operation '" + symbol + "' must have 2 operands.");
                }

                return new RelationalOperation(
                    Compiler.buildExpression(node.getChildren().get(0)),
                    Compiler.buildExpression(node.getChildren().get(1)),
                    opcode.intValue());
            }
        }

        throw new RuntimeException("Invalid relational operation.");
    }

    public RelationalOperation(Expression e1, Expression e2, int operation) {
        this.e1 = e1;
        this.e2 = e2;
        this.operation = operation;
    }

    public Class type(CompilationContext context) {
        return null;
    }

    public void emit(CompilationContext context) {
        // TODO: Add support for non-primitive types.

        Class operand1 = null;
        Class operand2 = null;

        this.e1.emit(context);
        operand1 = context.currentFrame().operandStack.peek();

        this.e2.emit(context);
        operand2 = context.currentFrame().operandStack.peek();

        // TODO: if !(operand1.isPrimitive()) and !(operand2.isPrimitive())
        // TODO: if !(oeprand1.isPrimitive()) and operand2.isPrimitive() .... auto boxing...?

        Class outputType = MathOperation.implicitConversion(operand1, operand2);
        if(outputType == null) {
            throw new RuntimeException("Invalid math operation. Cannot perform operation on a " + operand1 + " and a " + operand2);
        }

        if(!operand1.equals(operand2)) {
            // TODO: Optimize this. The issue I have right now is that "emit" adds all of the instructions
            // to a MethodVisitor rather than returning an InsnList so I cannot tell what the type is ahead of time.
            // Once I figure out how to unify those APIs I should optimze this. Or, replace them with a bunch of Static calls
            // to runtime methods that handle the math for me if unifying the APIs ais not worth it...

            // TODO: Only do this swapping if operand1 needs to be converted. If only operand2 needs to be convert you can get by with just appending an instructions.

            // TODO: The other issue that I should keep in mind here is that returning an InsnList may mess up if one of the operators
            // is a function call that needs to be weaved. That would screw a bunch of stuff up because the frame.operandStack no longer
            // matches reality. And I cannot re-run the compilation because I may be emitting custom classes.
            context.currentFrame().generator.cast(Type.getType(operand2), Type.getType(outputType));
            context.currentFrame().generator.swap(Type.getType(operand1), Type.getType(outputType));
            context.currentFrame().generator.cast(Type.getType(operand1), Type.getType(outputType));
            context.currentFrame().generator.swap(Type.getType(outputType), Type.getType(outputType));
        }

        Label trueLabel = context.currentFrame().generator.newLabel();
        Label endLabel = context.currentFrame().generator.newLabel();

        context.currentFrame().generator.ifCmp(Type.getType(outputType), this.operation, trueLabel);
        context.currentFrame().generator.push(false);
        context.currentFrame().generator.goTo(endLabel);
        context.currentFrame().generator.mark(trueLabel);
        context.currentFrame().generator.push(true);
        context.currentFrame().generator.mark(endLabel);

        context.currentFrame().operandStack.pop();
        context.currentFrame().operandStack.pop();

        context.currentFrame().operandStack.push(Boolean.TYPE);
    }
}
    