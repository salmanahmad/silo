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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;

import org.objectweb.asm.Type;
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
                identifier = Node.symbolListFromNode(Node.flattenTree(n, new Symbol(".")));

                if(identifier == null) {
                    identifier = null;
                    receiver = Compiler.buildExpression(label);
                }
            } else {
                identifier = null;
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

    public static int mostSpecificClass(Class c1, Class c2) {
        if(c1.equals(c2)) {
            return -1;
        }

        if(c1.isAssignableFrom(c2)) {
            return 1;
        }

        if(c2.isAssignableFrom(c1)) {
            return 0;
        }

        return -1;
    }

    public static int mostSpecificParametersFixedArgs(Class[] p1, Class[] p2) {
        int index = -1;

        if(p1.length == p2.length) {
            for(int i = 0; i < p1.length; i++) {
                Class class1 = p1[i];
                Class class2 = p2[i];

                if(index == -1) {
                    index = mostSpecificClass(class1, class2);
                } else {
                    if(index != mostSpecificClass(class1, class2)) {
                        return -1;
                    }
                }
            }
        }

        return index;
    }

    public static int mostSpecificParametersVarArgs(Class[] p1, Class[] p2) {
        if(p1.length == p2.length) {
            return mostSpecificParametersFixedArgs(p1, p2);
        }

        int index = -1;

        for(int i = 0; i < Math.max(p1.length, p2.length); i++) {
            Class class1 = null;
            Class class2 = null;

            if(i >= p1.length - 1) {
                class1 = p1[p1.length - 1].getComponentType();
            } else {
                class1 = p1[i];
            }

            if(i >= p2.length - 1) {
                class2 = p2[p2.length - 1].getComponentType();
            } else {
                class2 = p2[i];
            }

            if(index == -1) {
                index = mostSpecificClass(class1, class2);
            } else {
                if(index != mostSpecificClass(class1, class2)) {
                    return -1;
                }
            }
        }

        return index;
    }

    public static int resolveFunctionByArguments(Class[][] parameters, Class[] args) {
        Vector<Integer> options = new Vector<Integer>();

        int i = 0;
        for(Class[] expected : parameters) {
            if(typesMatch(expected, args)) {
                options.add(i);
            }

            i++;
        }



        int index = -1;
        for(i = 0; i < options.size(); i++) {
            if(i == 0) {
                index = 0;
            } else {
                int result = mostSpecificParametersFixedArgs(
                    parameters[options.get(index)],
                    parameters[options.get(i)]
                );

                if(result == 0) {
                    index = index;
                } else if(result == 1) {
                    index = i;
                } else {
                    index = -1;
                    break;
                }
            }
        }

        if(index == -1) {
            return -1;
        } else {
            return options.get(index);
        }
    }

    public static int resolveFunctionByAutoboxing(Class[][] parameters, Class[] args) {
        // TODO: Implement This
        // TODO: I may want to implement this with my type propogation algorithm...
        return -1;
    }

    // Assumes the last parameter in the parameter list is an array that represent that type of the
    // variable argument array.
    public static int resolveFunctionByVarArgs(Class[][] parameters, Class[] args) {
        Vector<Integer> options = new Vector<Integer>();

        int i = 0;
        for(Class[] expected : parameters) {
            boolean match = true;

            for(int j = 0; j < args.length; j++) {
                Class arg = null;
                Class e = null;

                arg = args[j];
                if(j >= expected.length - 1) {
                    e = expected[expected.length - 1].getComponentType();
                } else {
                    e = expected[j];
                }

                // TODO: isAssignableFrom does not handle boxing and unboxing
                if(e.isAssignableFrom(arg)) {
                    match = match && true;
                } else {
                    match = false;
                }
            }

            if(match) {
                options.add(i);
            }

            i++;
        }



        int index = -1;
        for(i = 0; i < options.size(); i++) {
            if(i == 0) {
                index = 0;
            } else {
                int result = mostSpecificParametersVarArgs(
                    parameters[options.get(index)],
                    parameters[options.get(i)]
                );

                if(result == 0) {
                    index = index;
                } else if(result == 1) {
                    index = i;
                } else {
                    index = -1;
                    break;
                }
            }
        }

        if(index == -1) {
            return -1;
        } else {
            return options.get(index);
        }
    }

    public static Class[][] convertMethodsToParameterLists(Method[] methods) {
        Class[][] classes = new Class[methods.length][];

        for(int i = 0; i < methods.length; i++) {
            classes[i] = methods[i].getParameterTypes();
        }

        return classes;
    }

    public static Class[][] convertConstructorsToParameterLists(Constructor[] constructors) {
        Class[][] classes = new Class[constructors.length][];

        for(int i = 0; i < constructors.length; i++) {
            classes[i] = constructors[i].getParameterTypes();
        }

        return classes;
    }

    public static Method resolveMethod(Class klass, String name, boolean isStatic, Class[] args) {
        Vector<Method> methods = new Vector<Method>();

        for(Method m : klass.getMethods()) {
            if(Modifier.isStatic(m.getModifiers()) != isStatic) {
                continue;
            }

            if(!Modifier.isPublic(m.getModifiers())) {
                continue;
            }

            if(m.getName().equals(name)) {
                methods.add(m);
            }
        }

        int index = -1;

        // Phase 1
        index = resolveFunctionByArguments(convertMethodsToParameterLists(methods.toArray(new Method[0])), args);
        if(index != -1) {
            return methods.get(index);
        }

        // Phase 2
        // TODO: Autoboxing

        // Phase 3
        Vector<Method> varArgsMethods = new Vector<Method>();
        for(Method method : methods) {
            if(method.isVarArgs()) {
                varArgsMethods.add(method);
            }
        }

        index = resolveFunctionByArguments(convertMethodsToParameterLists(varArgsMethods.toArray(new Method[0])), args);
        if(index != -1) {
            return varArgsMethods.get(index);
        }

        return null;
    }

    public static Constructor resolveConstructor(Class klass, Class[] args) {
        Vector<Constructor> constructors = new Vector<Constructor>();

        for(Constructor c : klass.getConstructors()) {
            if(!Modifier.isPublic(c.getModifiers())) {
                continue;
            }

            constructors.add(c);
        }

        int index = -1;

        // Phase 1
        index = resolveFunctionByArguments(convertConstructorsToParameterLists(constructors.toArray(new Constructor[0])), args);
        if(index != -1) {
            return constructors.get(index);
        }

        // Phase 2
        // TODO: Autoboxing

        // Phase 3
        Vector<Constructor> varArgsConstructors = new Vector<Constructor>();
        for(Constructor c : constructors) {
            if(c.isVarArgs()) {
                varArgsConstructors.add(c);
            }
        }

        index = resolveFunctionByArguments(convertConstructorsToParameterLists(varArgsConstructors.toArray(new Constructor[0])), args);
        if(index != -1) {
            return varArgsConstructors.get(index);
        }

        return null;
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        if(receiver == null) {
            // TODO: Handle local and imported variables and closures --- but that isn't here, is it? I can handle that out side the `if(receiver == null)` block...

            Vector result = Compiler.resolveIdentifierPath(identifier, context);

            if(result != null) {
                Class klass = (Class)result.get(0);
                Vector<Symbol> path = (Vector<Symbol>)result.get(1);

                if(path.size() == 0) {
                    // Native function or Constructor

                    // TODO: Handle arity overloading - Method.html#isVarArgs()
                    // TODO: Should I support type overloading?

                    if(Function.class.isAssignableFrom(klass)) {
                        Method method = Function.methodHandle(klass);

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

                        generator.invokeStatic(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(method));

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

                        Constructor constructor = resolveConstructor(klass, types.toArray(new Class[0]));
                        if(constructor == null) {
                            throw new RuntimeException("Could not find constructor");
                        }

                        generator.invokeConstructor(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(constructor));

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

                    Method method = resolveMethod(klass, symbol.toString(), true, types.toArray(new Class[0]));

                    if(method == null) {
                        throw new RuntimeException("Could not find function: " + symbol.toString());
                    }

                    generator.invokeStatic(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(method));

                    for(Expression e : arguments) {
                        frame.operandStack.pop();
                    }

                    frame.operandStack.push(method.getReturnType());

                    return;
                }
            }
        }

        Expression expression =  null;

        if(receiver == null) {
            expression = new Access(null, identifier);
        } else {
            expression = receiver;
        }

        expression.emit(context);
        Class operand = frame.operandStack.peek();

        if(operand.isArray()) {
            Vector<Class> classes = compileArguments(arguments, context);

            if(classes.size() != 1) {
                throw new RuntimeException("An array lookup requires a single parameter.");
            }

            if(!classes.get(0).equals(Integer.TYPE)) {
                throw new RuntimeException("An array lookup requires a single integer parameter. Was provided: " + classes.get(0));
            }

            generator.arrayLoad(Type.getType(operand.getComponentType()));

            frame.operandStack.pop();
            frame.operandStack.pop();
            frame.operandStack.push(operand.getComponentType());
        } else {
            // Dynamic function invocation
            // TODO: Remain cases...
            // TODO: Remember to throw exceptions if the symbol is legit not found.

            throw new RuntimeException("Dynamic invocation has not been implemented: " + operand);
        }
    }
}
