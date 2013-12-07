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

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

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

    public void emit(CompilationContext context, GeneratorAdapter generator) {
        this.e1.emit(context, generator);
        this.e2.emit(context, generator);

        // TODO: Implicit conversion rules

        generator.math(this.operation, Type.INT_TYPE);

        context.operandStack.pop();
        context.operandStack.pop();
        context.operandStack.push(Type.INT_TYPE);
    }
}
    