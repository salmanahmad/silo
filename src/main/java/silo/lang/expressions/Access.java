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
import java.lang.reflect.Field;

import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.GeneratorAdapter;

public class Access implements Expression {

    Object value;

    public Access(Object value) {
        // TODO: If I bring back the idea of a "ExpressionBuilder" I should use that here
        // to validate the value before accepting it. This will make it less likely to have to
        // check all issues in the emit.
        this.value = value;
    }

    public static void resolveSymbol(Symbol symbol, CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        if(frame.locals.containsKey(symbol)) {
            int local = frame.locals.get(symbol).intValue();
            Class scope = frame.localTypes.get(symbol);

            generator.visitVarInsn(Type.getType(scope).getOpcode(Opcodes.ILOAD), local);
            frame.operandStack.push(scope);
        } else {
            // TODO: Attempt to find Class reference by this name and return it
            throw new RuntimeException("Could not find local variable: " + symbol);
        }
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();


        // TODO: What about obscured packages? Do they stay obscured? In other words, suppose there are two class
        // foo.bar.Baz.a.Car and foo.bar.Baz. If the class "Baz" does not have a field "a" the current implementation
        // will throw an exception, it will not attempt to find package "foo.bar.Baz.a".


        if(value instanceof Symbol) {
            // Local variable or class reference...
            resolveSymbol((Symbol)value, context);
        } else if(value instanceof Node) {
            Node node = (Node)value;

            if(!(node.getSecondChild() instanceof Symbol)) {
                throw new RuntimeException("The access operator must take a symbol to look up. It was provided: " + node.getSecondChild());
            }

            Vector components = Node.flattenTree(node, new Symbol(".")).getChildren();

            Vector<Symbol> path = new Vector<Symbol>();
            for(Object component : components) {
                if(component instanceof Symbol) {
                    path.add((Symbol)component);
                } else {
                    path = null;
                    break;
                }
            }

            if(path != null) {
                if(frame.locals.containsKey(path.get(0))) {
                    // Compile first. getField on second.
                    path = new Vector<Symbol>();
                    path.add((Symbol)node.getSecondChild());

                    Expression expression = Compiler.buildExpression(node.getFirstChild());
                    expression.emit(context);
                } else {
                    Vector result = Compiler.resolveIdentifierPath(path, context);

                    if(result == null) {
                        throw new RuntimeException("Could not find symbol: " + path.toString());
                    }

                    Class scope = (Class)result.get(0);
                    path = (Vector<Symbol>)result.get(1);

                    if(path.size() == 0) {
                        // TODO: Return class reference...
                        throw new RuntimeException("Unimplemented");
                    } else {
                        // Getstatic
                        Symbol symbol = path.remove(0);

                        try {
                            Field field = scope.getField(symbol.toString());
                            generator.getStatic(Type.getType(scope), field.getName(), Type.getType(field.getType()));
                            frame.operandStack.push(field.getType());
                        } catch(NoSuchFieldException e) {
                            throw new RuntimeException("Could not find field: " + symbol);
                        }
                    }
                }
            } else {
                // Compile first. getField on second.
                path = new Vector<Symbol>();
                path.add((Symbol)node.getSecondChild());

                Expression expression = Compiler.buildExpression(node.getFirstChild());
                expression.emit(context);
            }

            for(Symbol symbol : path) {
                try {
                    Class operand = frame.operandStack.pop();
                    Field field = operand.getField(symbol.toString());

                    generator.getField(Type.getType(operand), field.getName(), Type.getType(field.getType()));
                    frame.operandStack.push(field.getType());
                } catch(NoSuchFieldException e) {
                    throw new RuntimeException("No such field was found: " + symbol.toString());
                }
            }
        } else {
            throw new RuntimeException("Invalid access form.");
        }
    }
}
