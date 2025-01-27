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
    boolean willMutate;

    public Access(Object value) {
        // TODO: If I bring back the idea of a "ExpressionBuilder" I should use that here
        // to validate the value before accepting it. This will make it less likely to have to
        // check all issues in the emit.
        this.value = value;
    }

    public Access(Object value, boolean willMutate) {
        this.value = value;
        this.willMutate = willMutate;
    }

    public void resolveSymbol(Symbol symbol, CompilationContext context) {
        resolveSymbol(symbol, context, true);
    }

    private void resolveSymbol(Symbol symbol, CompilationContext context, boolean shouldEmit) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        if(frame.locals.containsKey(symbol)) {
            int local = frame.locals.get(symbol).intValue();
            Class scope = frame.localTypes.get(symbol);

            if(shouldEmit) {
                generator.visitVarInsn(Type.getType(scope).getOpcode(Opcodes.ILOAD), local);

                if(willMutate && Structure.class.isAssignableFrom(scope)) {
                    generator.invokeVirtual(Type.getType(Structure.class), org.objectweb.asm.commons.Method.getMethod("silo.lang.Structure copyForMutation()"));
                    generator.checkCast(Type.getType(scope));
                    generator.dup();
                    generator.visitVarInsn(Type.getType(scope).getOpcode(Opcodes.ISTORE), local);
                }
            }

            frame.operandStack.push(scope);
        } else {
            Class klass = Compiler.resolveType(symbol, context);

            if(klass != null) {
                if(Function.class.isAssignableFrom(klass)) {
                    // Function reference
                    if(shouldEmit) {
                        generator.newInstance(Type.getType(klass));
                        generator.dup();
                        generator.invokeConstructor(
                            Type.getType(klass),
                            Method.getMethod("void <init> ()")
                        );
                    }

                    frame.operandStack.push(Function.class);
                } else {
                    // Class reference
                    if(shouldEmit) {
                        generator.visitLdcInsn(Type.getType(klass));
                    }

                    frame.operandStack.push(Class.class);
                }
            } else {
                throw new RuntimeException("Could not find local variable: " + symbol);
            }
        }
    }

    public Class type(CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        int size = frame.operandStack.size();

        emit(context, false);

        if(frame.operandStack.size() - size != 1) {
            throw new RuntimeException("Error!");
        }

        return frame.operandStack.pop();
    }

    public Object scaffold(CompilationContext context) {
        if(value instanceof Node) {
            return Compiler.scaffoldNodeChildren((Node)value, context);
        } else {
            // TODO: Should I replace all the value with aliases here?
            // that way I can use aliases for constructors as well...
            return value;
        }
    }

    public void emit(CompilationContext context) {
        emit(context, true);
    }

    private void emit(CompilationContext context, boolean shouldEmit) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();


        // TODO: What about obscured packages? Do they stay obscured? In other words, suppose there are two class
        // foo.bar.Baz.a.Car and foo.bar.Baz. If the class "Baz" does not have a field "a" the current implementation
        // will throw an exception, it will not attempt to find package "foo.bar.Baz.a".


        if(value instanceof Symbol) {
            // Local variable or class reference...
            resolveSymbol((Symbol)value, context, shouldEmit);
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

                    //Expression expression = Compiler.buildExpression(node.getFirstChild());
                    Expression expression = new Access(node.getFirstChild(), willMutate);

                    if(shouldEmit) {
                        expression.emit(context);
                    } else {
                        frame.operandStack.push(expression.type(context));
                    }
                } else {
                    Vector result = Compiler.resolveIdentifierPath(path, context);

                    if(result == null) {
                        throw new RuntimeException("Could not find symbol: " + path.toString());
                    }

                    Class scope = (Class)result.get(0);
                    path = (Vector<Symbol>)result.get(1);

                    if(path.size() == 0) {
                        if(Function.class.isAssignableFrom(scope)) {
                            // Function reference
                            if(shouldEmit) {
                                generator.newInstance(Type.getType(scope));
                                generator.dup();
                                generator.invokeConstructor(
                                    Type.getType(scope),
                                    Method.getMethod("void <init> ()")
                                );
                            }

                            frame.operandStack.push(Function.class);
                        } else {
                            // Class reference
                            if(shouldEmit) {
                                generator.visitLdcInsn(Type.getType(scope));
                            }

                            frame.operandStack.push(Class.class);
                        }
                    } else {
                        // Getstatic
                        Symbol symbol = path.remove(0);

                        try {
                            Field field = scope.getField(symbol.toString());

                            if(shouldEmit) {
                                generator.getStatic(Type.getType(scope), field.getName(), Type.getType(field.getType()));
                            }

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

                if(shouldEmit) {
                    expression.emit(context);
                } else {
                    frame.operandStack.push(expression.type(context));
                }
            }

            for(Symbol symbol : path) {
                try {
                    Class operand = frame.operandStack.pop();
                    Field field = operand.getField(symbol.toString());

                    if(shouldEmit) {
                        if(willMutate && Structure.class.isAssignableFrom(operand)) {
                            generator.invokeVirtual(Type.getType(operand), new org.objectweb.asm.commons.Method("get_for_mutation_" + symbol.toString(), Type.getType(field.getType()), new Type[0]));
                        } else {
                            generator.getField(Type.getType(operand), field.getName(), Type.getType(field.getType()));
                        }
                    }

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
