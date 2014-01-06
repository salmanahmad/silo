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

    public final Node node;

    public Invoke(Node node) {
        this.node = node;
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
            boolean match = true;

            if(expected.length != args.length) {
                match = false;
                i++;
                continue;
            }

            for(int j = 0; j < expected.length; j++) {
                Class e = expected[j];
                Class p = args[j];

                if(!(e.isAssignableFrom(p))) {
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

        index = resolveFunctionByVarArgs(convertConstructorsToParameterLists(varArgsConstructors.toArray(new Constructor[0])), args);
        if(index != -1) {
            return varArgsConstructors.get(index);
        }

        return null;
    }

    public static Vector<Class> compileArguments(Vector<Expression> arguments, CompilationContext context) {
        return compileArguments(arguments, context, true);
    }

    public static Vector<Class> compileArguments(Vector<Expression> arguments, CompilationContext context, boolean shouldEmit) {
        CompilationFrame frame = context.currentFrame();

        Vector<Class> types = new Vector<Class>();

        for(Expression e : arguments) {
            if(shouldEmit) {
                e.emit(context);
            } else {
                frame.operandStack.push(e.type(context));
            }

            types.add(frame.operandStack.peek());
        }

        return types;
    }

    public static Vector<Class> argumentTypes(Vector<Expression> arguments, CompilationContext context) {
        Vector<Class> types = new Vector<Class>();

        for(Expression e : arguments) {
            types.add(e.type(context));
        }

        return types;
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

    public void emit(CompilationContext context) {
        emit(context, true);
    }

    private void emit(CompilationContext context, boolean shouldEmit) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();


        Vector<Symbol> identifier = new Vector<Symbol>();
        Vector<Expression> arguments = new Vector<Expression>();
        Object label = node.getLabel();

        if(node.getChildren() != null) {
            for(Object child : node.getChildren()) {
                arguments.add(Compiler.buildExpression(child));
            }
        }


        if(label instanceof Symbol) {
            identifier.add((Symbol)label);
        } else if(label instanceof Node) {
            Node n = (Node)label;
            if(n.getLabel().equals(new Symbol("."))) {
                identifier = Node.symbolListFromNode(Node.flattenTree(n, new Symbol(".")));
            }
        } else {
            throw new RuntimeException("Invalid function invocation.");
        }



        if(identifier != null) {
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

                        Vector<Class> types = compileArguments(arguments, context, shouldEmit);

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

                        if(shouldEmit) {
                            generator.invokeStatic(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(method));
                        }

                        for(Expression e : arguments) {
                            frame.operandStack.pop();
                        }

                        frame.operandStack.push(method.getReturnType());

                        return;
                    } else {
                        // TODO: Add another "else if" clause that checks for a "record" or "type"

                        if(shouldEmit) {
                            generator.newInstance(Type.getType(klass));
                            Compiler.dup(klass, generator);
                        }

                        frame.operandStack.push(klass);
                        frame.operandStack.push(klass);

                        Vector<Class> types = argumentTypes(arguments, context);

                        Constructor constructor = resolveConstructor(klass, types.toArray(new Class[0]));
                        if(constructor == null) {
                            throw new RuntimeException("Could not find constructor");
                        }

                        Class[] params = constructor.getParameterTypes();
                        boolean shouldVarArgs = false;

                        if(constructor.isVarArgs()) {
                            if(types.size() > params.length) {
                                shouldVarArgs = true;
                            } else if(types.size() == params.length) {
                                shouldVarArgs = params[params.length - 1].equals(types.get(params.length - 1));
                                shouldVarArgs = !shouldVarArgs;
                            } else {
                                throw new RuntimeException("Error!");
                            }
                        }

                        if(shouldVarArgs) {
                            int count = arguments.size() - (params.length - 1);

                            for(int i = 0; i < arguments.size() - count; i++) {
                                Expression e = arguments.get(i);

                                if(shouldEmit) {
                                    e.emit(context);
                                } else {
                                    frame.operandStack.push(e.type(context));
                                }
                            }

                            //new Array...
                            Class arrayClass = params[params.length - 1];

                            if(shouldEmit) {
                                generator.push(count);
                                generator.newArray(Type.getType(arrayClass.getComponentType()));
                            }

                            frame.operandStack.push(arrayClass);

                            int index = 0;
                            for(int i = arguments.size() - count; i < arguments.size(); i++) {
                                if(shouldEmit) {
                                    generator.dup();
                                    generator.push(index);
                                }

                                frame.operandStack.push(arrayClass);
                                frame.operandStack.push(Integer.TYPE);

                                Expression e = arguments.get(i);

                                if(shouldEmit) {
                                    e.emit(context);
                                    generator.arrayStore(Type.getType(arrayClass.getComponentType()));
                                } else {
                                    frame.operandStack.push(e.type(context));
                                }

                                frame.operandStack.pop();
                                frame.operandStack.pop();
                                frame.operandStack.pop();

                                index++;
                            }
                        } else {
                            compileArguments(arguments, context, shouldEmit);
                        }

                        if(shouldEmit) {
                            generator.invokeConstructor(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(constructor));
                        }

                        // Remove the class reference that was used to invoke the constructor
                        frame.operandStack.pop();

                        if(shouldVarArgs) {
                            for(int i = 0; i < params.length; i++) {
                                frame.operandStack.pop();
                            }
                        } else {
                            for(Expression e : arguments) {
                                frame.operandStack.pop();
                            }
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

                    if(shouldEmit) {
                        generator.invokeStatic(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(method));
                    }

                    for(Expression e : arguments) {
                        frame.operandStack.pop();
                    }

                    frame.operandStack.push(method.getReturnType());

                    return;
                }
            }
        }



        Expression expression = Compiler.buildExpression(label);
        if(shouldEmit) {
            expression.emit(context);
        } else {
            frame.operandStack.push(expression.type(context));
        }

        Class operand = frame.operandStack.peek();
        if(operand.isArray()) {
            Vector<Class> classes = compileArguments(arguments, context, shouldEmit);

            if(classes.size() != 1) {
                throw new RuntimeException("An array lookup requires a single parameter.");
            }

            if(!classes.get(0).equals(Integer.TYPE)) {
                throw new RuntimeException("An array lookup requires a single integer parameter. Was provided: " + classes.get(0));
            }

            if(shouldEmit) {
                generator.arrayLoad(Type.getType(operand.getComponentType()));
            }

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
