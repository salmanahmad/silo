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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.GeneratorAdapter;

public class Assign implements Expression {

    public final Symbol identifier;
    public final Vector<Symbol> type;
    public final Expression value;

    public static Assign build(Node node) {
        if(node.getChildren().size() != 2) {
            throw new RuntimeException("Assignment requires at least 2 arguments.");
        }

        Symbol identifier = null;
        Vector<Symbol> type = null;
        Expression value = null;

        value = Compiler.buildExpression(node.getSecondChild());

        Object o = node.getFirstChild();
        if(o instanceof Symbol) {
            identifier = (Symbol)o;
        } else if(o instanceof Node) {
            Object first = ((Node)o).getFirstChild();
            Object second = ((Node)o).getSecondChild();

            if(first instanceof Symbol) {
                identifier = (Symbol)first;
            } else {
                // TODO: Implement nested assignment.
                throw new RuntimeException("Invalid assignment form. Nested assignments are not implemented yet. The variable name must be a symbol for now.");
            }

            if(second instanceof Symbol) {
                type = new Vector<Symbol>();
                type.add((Symbol)second);
            } else if(second instanceof Node) {
                type = Node.symbolListFromNode(Node.flattenTree((Node)second, new Symbol(".")));

                if(type == null) {
                    throw new RuntimeException("Invalid type name");
                }
            } else {
                throw new RuntimeException("Invalid type name");
            }

        } else {
            throw new RuntimeException("Invalid assignment form. The first argument must be a variable identifier or a typed expression");
        }

        return new Assign(identifier, type, value);
    }

    public Assign(Symbol identifier, Vector<Symbol> type, Expression value) {
        this.identifier = identifier;
        this.type = type;
        this.value = value;
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        int local = -1;

        // Resolve the type's class
        Class typeClass = null;
        if(type != null) {
            typeClass = Compiler.resolveType(type, context);

            if(typeClass == null) {
                throw new RuntimeException("Could not find symbol: " + type.toString());
            }
        }

        // Check if the variable is already defined
        if(!frame.locals.containsKey(identifier)) {
            // If it is not then define it with the type

            if(typeClass == null) {
                // TODO: Change this to the Var
                typeClass = Object.class;
            }

            local = frame.newLocal(identifier, typeClass);
        } else {
            // If it is defined, make sure the types match up.

            local = frame.locals.get(identifier).intValue();

            if(typeClass != null) {
                Class klass = frame.localTypes.get(identifier);
                if(!klass.equals(typeClass)) {
                    throw new RuntimeException("Attempting to re-define variable " + identifier.toString() + " with a different type.");
                }
            }

            if(typeClass == null) {
                typeClass = frame.localTypes.get(identifier);
            }
        }

        // Compile the value
        if(value != null) {
            value.emit(context);
        } else {
            generator.push((String)null);
            frame.operandStack.push(Object.class);
        }

        Class valueClass = frame.operandStack.peek();

        if(!typeClass.isAssignableFrom(valueClass)) {
            // TODO: Abstract this out so that it handles things likes autoboxing, and conversion, etc.
            throw new RuntimeException("Invalid assignment from type " + valueClass + " to " + typeClass);
        }

        // Return the value from the assignment for cascading assigments
        Compiler.dup(frame.operandStack.peek(), generator);

        // Perform assignment keeping one of the values on the stack
        generator.visitVarInsn(Type.getType(typeClass).getOpcode(Opcodes.ISTORE), local);
    }
}
