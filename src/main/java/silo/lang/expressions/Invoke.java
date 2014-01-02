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
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.GeneratorAdapter;

public class Invoke implements Expression {

    public final Expression receiver;
    public final Vector<Symbol> identifier;
    public final Vector<Expression> arguments;

    public static Invoke build(Node node) {
        Expression receiver = null;
        Vector<Symbol> identifier = null;
        Vector<Expression> arguments = new Vector<Expression>();

        Object label = node.getLabel();
        if(label instanceof Symbol) {
            identifier = new Vector<Symbol>();
            identifier.add((Symbol)label);
        } else if(label instanceof Node) {
            Node n = (Node)label;

            if(n.getLabel().equals(new Symbol("."))) {
                Node components = Node.flattenTree(n, new Symbol("."));
                identifier = Node.symbolListFromNode(components);

                if(identifier == null) {
                    receiver = Compiler.buildExpression(label);
                }
            } else {
                receiver = Compiler.buildExpression(label);
            }
        } else {
            throw new RuntimeException("Unhandled case.");
        }

        if(node.getChildren() != null) {
            arguments = new Vector<Expression>();
            for(Object child : node.getChildren()) {
                arguments.add(Compiler.buildExpression(child));
            }
        }

        return new Invoke(receiver, identifier, arguments);
    }

    public Invoke(Expression receiver, Vector<Symbol> identifier, Vector<Expression> arguments) {
        if(receiver != null && identifier != null) {
            throw new RuntimeException("Receiver and identifier cannot both be non-null.");
        }

        if(receiver == null && identifier == null) {
            throw new RuntimeException("Receiver and identifier cannot both be null.");
        }

        this.receiver = receiver;
        this.identifier = identifier;
        this.arguments = arguments;
    }

    public static Vector<Class> compileArguments(Vector<Expression> arguments, CompilationContext context) {
        CompilationFrame frame = context.currentFrame();

        Vector<Class> types = new Vector<Class>();

        for(Expression e : arguments) {
            e.emit(context);
            types.add(frame.operandStack.peek());
        }

        return types;
    }

    public static boolean typesMatch(Class[] expected, Class[] provided) {
        if(expected.length != provided.length) {
            return false;
        }

        for(int i = 0; i < expected.length; i++) {
            Class e = expected[i];
            Class p = provided[i];

            if(!(e.isAssignableFrom(p))) {
                return false;
            }
        }

        return true;
    }

    public static Class mostSpecificClass(Class c1, Class c2) {
        if(c1.equals(c2)) {
            return null;
        }

        if(c1.isAssignableFrom(c2)) {
            return c2;
        }

        if(c2.isAssignableFrom(c1)) {
            return c1;
        }

        return null;
    }

    public static int mostSpecificParameters(Class[] p1, Class[] p2) {
        int index = -1;

        for(int i = 0; i < p1.length; i++) {
            Class class1 = p1[i];
            Class class2 = p2[i];

            Class klass = mostSpecificClass(class1, class2);

            if(klass != null) {
                if(class1.equals(klass)) {
                    if(index == -1) {
                        index = 0;
                    } else {
                        if(index != 0) {
                            return -1;
                        }
                    }
                } else if(class2.equals(klass)) {
                    if(index == -1) {
                        index = 1;
                    } else {
                        if(index != 1) {
                            return -1;
                        }
                    }
                }
            }
        }

        return index;
    }

    public static int mostSpecificParameters(Vector<Class[]> params) {
        int index = -1;
        int count = -1;

        for(Class[] p : params) {
            if(count == -1) {
                count = p.length;
            } else {
                if(count != p.length) {
                    // TODO: Fix this with varags
                    throw new RuntimeException("Parameter list hold a different number of arguments.");
                }
            }
        }

        for(int i = 0; i < params.size(); i++) {
            if(i == 0) {
                index = 0;
            } else {
                index = mostSpecificParameters(params.get(index), params.get(i));

                if(index == -1) {
                    break;
                }
            }
        }

        return index;
    }

    public static java.lang.reflect.Method getMethod(Class klass, String name, boolean isStatic, Class[] args) {
        Vector<Class[]> options = new Vector<Class[]>();
        Vector<java.lang.reflect.Method> methods = new Vector<java.lang.reflect.Method>();

        for(java.lang.reflect.Method m : klass.getMethods()) {
            if(java.lang.reflect.Modifier.isStatic(m.getModifiers()) != isStatic) {
                continue;
            }

            if(!java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                continue;
            }

            if(m.getName().equals(name)) {
                Class[] expected = m.getParameterTypes();

                if(typesMatch(expected, args)) {
                    methods.add(m);
                    options.add(expected);
                }
            }
        }

        int index = mostSpecificParameters(options);

        if(index == -1) {
            return null;
        } else {
            return methods.get(index);
        }
    }

    public static java.lang.reflect.Constructor getConstructor(Class klass, Class[] args) {
        Vector<Class[]> options = new Vector<Class[]>();
        Vector<java.lang.reflect.Constructor> ctors = new Vector<java.lang.reflect.Constructor>();

        for(java.lang.reflect.Constructor c : klass.getConstructors()) {
            if(!java.lang.reflect.Modifier.isPublic(c.getModifiers())) {
                continue;
            }

            Class[] expected = c.getParameterTypes();

            if(typesMatch(expected, args)) {
                ctors.add(c);
                options.add(expected);
            }
        }

        int index = mostSpecificParameters(options);

        if(index == -1) {
            return null;
        } else {
            return ctors.get(index);
        }
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        if(receiver == null) {
            // TODO: Handle local and imported variables

            Vector result = Compiler.resolveIdentifierPath(identifier, context);

            if(result != null) {
                Class klass = (Class)result.get(0);
                Vector<Symbol> path = (Vector<Symbol>)result.get(1);

                if(path.size() == 0) {
                    // Native function or Constructor

                    // TODO: Handle arity overloading - Method.html#isVarArgs()
                    // TODO: Should I support type overloading?

                    if(Function.class.isAssignableFrom(klass)) {
                        java.lang.reflect.Method method = Function.methodHandle(klass);

                        Vector<Class> types = compileArguments(arguments, context);

                        if(types.size() != method.getParameterTypes().length) {
                            throw new RuntimeException("Arity mismatch.");
                        }

                        for(int i = 0; i < types.size(); i++) {
                            Class expectedType = method.getParameterTypes()[i];
                            Class providedType = types.get(i);

                            if(!(expectedType.isAssignableFrom(providedType))) {
                                throw new RuntimeException("Parameter mismatch in " + klass + ". Expected: " + expectedType + " Provided: " + providedType);
                            }
                        }

                        generator.invokeStatic(Type.getType(klass), Method.getMethod(method));

                        for(Expression e : arguments) {
                            frame.operandStack.pop();
                        }

                        frame.operandStack.push(method.getReturnType());

                        return;
                    } else {
                        // TODO: Add another "else if" clause that checks for a "record" or "type"

                        generator.newInstance(Type.getType(klass));
                        Compiler.dup(klass, generator);
                        frame.operandStack.push(klass);

                        Vector<Class> types = compileArguments(arguments, context);

                        java.lang.reflect.Constructor constructor = getConstructor(klass, types.toArray(new Class[0]));
                        if(constructor == null) {
                            throw new RuntimeException("Could not find constructor");
                        }

                        generator.invokeConstructor(Type.getType(klass), Method.getMethod(constructor));

                        for(Expression e : arguments) {
                            frame.operandStack.pop();
                        }

                        return;
                    }
                } else if(path.size() == 1) {
                    // Java static method

                    // TODO: Handle arity overloading - Method.html#isVarArgs()

                    Symbol symbol = path.get(0);
                    Vector<Class> types = compileArguments(arguments, context);

                    java.lang.reflect.Method method = getMethod(klass, symbol.toString(), true, types.toArray(new Class[0]));

                    if(method == null) {
                        throw new RuntimeException("Could not find function: " + symbol.toString());
                    }

                    generator.invokeStatic(Type.getType(klass), Method.getMethod(method));

                    for(Expression e : arguments) {
                        frame.operandStack.pop();
                    }

                    frame.operandStack.push(method.getReturnType());

                    return;
                }
            }
        }

        // Dynamic function invocation
        // TODO: Remain cases...
        // TODO: Remember to throw exceptions if the symbol is legit not found.

        throw new RuntimeException("Dynamic invocation has not been implemented: " + identifier);
    }
}
