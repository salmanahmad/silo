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

import java.util.Arrays;
import java.util.Vector;
import java.util.Set;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

public class Invoke implements Expression {

    public final Node node;

    public Invoke(Node node) {
        this.node = node;
    }

    public static int mostSpecificClass(Class c1, Class c2) {
        if(c1.equals(c2)) {
            // If they are equal then neither is more specific than the other
            return 0;
        }

        if(c1.isAssignableFrom(c2)) {
            // If c1 can be assigned from c2 that means that c1 "isa" c2.
            // Since they are not equal (we checked for that already) it must
            // be the case the c2 is more "specific"
            return 2;
        }

        if(c2.isAssignableFrom(c1)) {
            // Logic follows here...
            return 1;
        }

        // They are incompatible types.
        return -1;
    }

    public static int mostSpecificParametersFixedArgs(Class[] p1, Class[] p2) {
        // Start off assuming that the args are equivalent
        int index = 0;

        if(p1.length == p2.length) {
            for(int i = 0; i < p1.length; i++) {
                Class class1 = p1[i];
                Class class2 = p2[i];

                int temp = mostSpecificClass(class1, class2);

                if(temp == -1) {
                    // If the classes are incompatible return -1
                    return -1;
                } else if(temp != 0){
                    // If the classes are equal (temp == 0) then ignore it

                    if(index == 0) {
                        // If neither p1 or p2 is more specific then lets pick "temp" for now
                        index = temp;
                    } else if(index != temp) {
                        // The more specific argument is from the other argument list
                        // Hence they are not compatible
                        return -1;
                    }
                }
            }
        } else {
            // If the arguments are of different lengths then obviously they are not compatible
            return -1;
        }

        return index;
    }

    public static int mostSpecificParametersVarArgs(Class[] p1, Class[] p2) {
        if(p1.length == p2.length) {
            return mostSpecificParametersFixedArgs(p1, p2);
        } else {
            int max = Math.max(p1.length, p2.length);
            Class[] newP1 = new Class[max];
            Class[] newP2 = new Class[max];

            for(int i = 0; i < max; i++) {
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

                newP1[i] = class1;
                newP2[i] = class2;
            }

            return mostSpecificParametersFixedArgs(newP1, newP2);
        }
    }

    // TODO: This method is very similiar to resolveFunctionByVarArgs.
    // Consider refactoring and consolidating them together.
    public static int resolveFunctionByArguments(Class[][] parameters, Class[] args) {
        Vector<Integer> options = new Vector<Integer>();

        // First filter out the parameters that are not valid for the args
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

                if(!Compiler.isValidAssignment(e, p)) {
                    match = false;
                }
            }

            if(match) {
                options.add(i);
            }

            i++;
        }

        // Second, we have to find the most specific arguments
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
                    // If they are equal we are going to pick the first method, hence we do not change index
                    index = index;
                } else if(result == 1) {
                    index = index;
                } else if(result == 2) {
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

                // TODO: isValidAssignment does not handle boxing and unboxing
                if(Compiler.isValidAssignment(e, arg)) {
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
                    // If they are equal we are going to pick the first method, hence we do not change index
                    index = index;
                } else if(result == 1) {
                    index = index;
                } else if(result == 2) {
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
        // Rules from: JLS 15.12
        // http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12

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

        index = resolveFunctionByVarArgs(convertMethodsToParameterLists(varArgsMethods.toArray(new Method[0])), args);
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

    public static void compileArguments(Class[] params, Vector<Expression> arguments, CompilationContext context, boolean shouldEmit) {
        CompilationFrame frame = context.currentFrame();

        if(params.length != arguments.size()) {
            throw new RuntimeException("Arguments do not match the parameter length. Arity mismatch.");
        }

        for(int i = 0; i < params.length; i++) {
            Expression e = arguments.get(i);

            if(shouldEmit) {
                e.emit(context);
            } else {
                frame.operandStack.push(e.type(context));
            }

            Compiler.autobox(params[i], context, shouldEmit);
        }
    }

    public static void compileVariableArguments(Class[] params, Vector<Expression> arguments, CompilationContext context, boolean shouldEmit) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = context.currentFrame().generator;

        int count = arguments.size() - (params.length - 1);

        for(int i = 0; i < arguments.size() - count; i++) {
            Expression e = arguments.get(i);

            if(shouldEmit) {
                e.emit(context);
            } else {
                frame.operandStack.push(e.type(context));
            }

            Compiler.autobox(params[i], context, shouldEmit);
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
                Compiler.autobox(arrayClass.getComponentType(), context, shouldEmit);
                generator.arrayStore(Type.getType(arrayClass.getComponentType()));
            } else {
                frame.operandStack.push(e.type(context));
                Compiler.autobox(e.type(context), context, shouldEmit);
            }

            frame.operandStack.pop();
            frame.operandStack.pop();
            frame.operandStack.pop();

            index++;
        }
    }


    public static void compileVariableArgumentsForNativeFunction(Class[] params, Vector<Expression> arguments, CompilationContext context, boolean shouldEmit) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = context.currentFrame().generator;

        for(int i = 0; i < params.length - 1; i++) {
            Expression e = arguments.get(i);

            if(shouldEmit) {
                e.emit(context);
            } else {
                frame.operandStack.push(e.type(context));
            }
        }


        if(shouldEmit) {
            generator.invokeStatic(
                Type.getType(PersistentVector.class),
                new org.objectweb.asm.commons.Method(
                    "emptyVector",
                    Type.getType(PersistentVector.class),
                    new Type[0]
                )
            );
        }

        frame.operandStack.push(PersistentVector.class);


        for(int i = params.length - 1; i < arguments.size(); i++) {
            Expression e = arguments.get(i);

            if(shouldEmit) {
                e.emit(context);
            } else {
                frame.operandStack.push(e.type(context));
            }

            if(shouldEmit) {
                generator.invokeVirtual(
                    Type.getType(PersistentVector.class),
                    new org.objectweb.asm.commons.Method(
                        "cons",
                        Type.getType(PersistentVector.class),
                        new Type[] { Type.getType(Object.class) }
                    )
                );
            }

            frame.operandStack.pop();
        }
    }

    public static Vector<Class> argumentTypes(Vector<Expression> arguments, CompilationContext context) {
        Vector<Class> types = new Vector<Class>();

        for(Expression e : arguments) {
            types.add(e.type(context));
        }

        return types;
    }

    public static boolean shouldUseVarArgs(Class[] params, Class[] args) {
        boolean shouldVarArgs = false;

        if(args.length > params.length) {
            shouldVarArgs = true;
        } else if(args.length == params.length - 1) {
            shouldVarArgs = true;
        } else if(args.length == params.length) {
            // TODO: Should this be Compiler.assignmentValidation?
            shouldVarArgs = params[params.length - 1].equals(args[params.length - 1]);
            shouldVarArgs = !shouldVarArgs;
        } else {
            throw new RuntimeException("Error!");
        }

        return shouldVarArgs;
    }

    public static void performNonResumableInvoke(CompilationContext context, Method method, Vector<Expression> arguments) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        Class klass = method.getDeclaringClass();
        Class[] params = method.getParameterTypes();
        Vector<Class> types = argumentTypes(arguments, context);
        boolean shouldVarArgs = false;

        if(method.isVarArgs()) {
            shouldVarArgs = shouldUseVarArgs(params, types.toArray(new Class[0]));
        }

        if(shouldVarArgs) {
            compileVariableArguments(params, arguments, context, true);
        } else {
            compileArguments(arguments, context, true);
        }

        if(Modifier.isStatic(method.getModifiers())) {
            generator.invokeStatic(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(method));
        } else {
            if(klass.isInterface()) {
                generator.invokeInterface(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(method));
            } else {
                generator.invokeVirtual(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(method));
            }
        }

        // Pop the arguments
        for(int i = 0; i < params.length; i++) {
            frame.operandStack.pop();
        }

        // Pop the receiver
        if(!Modifier.isStatic(method.getModifiers())) {
            frame.operandStack.pop();
        }

        // Add the return type
        if(method.getReturnType().equals(Void.TYPE)) {
            generator.push((String)null);
            frame.operandStack.push(Null.class);
        } else {
            frame.operandStack.push(method.getReturnType());
        }
    }

    public static void performResumableInvoke(CompilationContext context, Method method, Vector<Expression> arguments) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        Class klass = method.getDeclaringClass();
        boolean isNative = Function.class.isAssignableFrom(klass);
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        Class[] params = method.getParameterTypes();
        Vector<Class> types = argumentTypes(arguments, context);
        params = Arrays.copyOfRange(params, 1, params.length);

        boolean isVarArg = false;
        if(isNative) {
            if(isStatic) {
                // Direct function invocation
                isVarArg = Function.isVarArgs(klass);
            } else {
                // Invoking through a function handle
                // Right now all function handles are invoked with varargs. This may change.
                isVarArg = true;
            }
        } else {
            isVarArg = method.isVarArgs();
        }



        Label resumeSite = generator.newLabel();
        Label preCallSite = generator.newLabel();
        Label callSite = generator.newLabel();



        // ###
        // ### Skip over the resume point during normal execution
        generator.goTo(preCallSite);



        // ###
        // ### Resume Site - Load the execution context and dummy values for the invocation
        int programCounter = 0;
        generator.mark(resumeSite);

        for(int i = 0; i < frame.operandStack.size(); i++) {
            Class operandType = frame.operandStack.get(i);
            Compiler.pushInitializationValue(operandType, generator);
        }

        if(!isStatic) {
            // If this call site is virtual, I need to load the "reciever"

            // Pop the dummy value I just inserted onto the stack. It is a function handle so
            // I do not need to do "Compiler.pop()" and worry about category2 data types.
            generator.pop();

            Compiler.loadExecutionContext(context);
            generator.invokeVirtual(Type.getType(ExecutionContext.class), org.objectweb.asm.commons.Method.getMethod("silo.lang.ExecutionFrame getCurrentFrame()"));
            generator.getField(Type.getType(ExecutionFrame.class), "stack", Type.getType(Object[].class));
            generator.push(frame.operandStack.size() - 1);
            generator.arrayLoad(Type.getType(klass));
            generator.checkCast(Type.getType(klass));

            // Duplicate the reciever
            generator.dup();
        }
        Compiler.loadExecutionContext(context);

        for(Class param : params) {
            Compiler.pushInitializationValue(param, generator);
        }

        // Skip over the rest and go to the call site
        generator.goTo(callSite);



        // ###
        // ### Pre Call Site - Load the execution context and the actual parameters for the invocation
        generator.mark(preCallSite);

        if(!isStatic) {
            // Duplicate the receiever. It will be popped off below during RUNNING
            generator.dup();
            frame.operandStack.push(frame.operandStack.peek());
        }

        Compiler.loadExecutionContext(context);
        frame.operandStack.push(ExecutionContext.class);

        if(isVarArg) {
            // TODO: Perhaps "apply" should follow the same conventions as "invoke" for varargs so that I do not need
            // to branch out here and use different method calling conventions.
            // TODO: Making apply and invoke have the same conventions will also mean that the varargs check
            // earlier in this function can be combined and not need the branches that I take...

            if(isNative && isStatic) {
                Invoke.compileVariableArgumentsForNativeFunction(params, arguments, context, true);
            } else {
                Invoke.compileVariableArguments(params, arguments, context, true);
            }
        } else {
            Invoke.compileArguments(params, arguments, context, true);
        }



        // ###
        // ### Actual Call Site
        generator.mark(callSite);

        Compiler.loadExecutionContext(context);
        generator.invokeVirtual(Type.getType(ExecutionContext.class), org.objectweb.asm.commons.Method.getMethod("void beginCall()"));

        if(isStatic) {
            generator.invokeStatic(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(method));
        } else {
            // TODO: Support calls with invokeSpecial?
            generator.invokeVirtual(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(method));
        }

        if(method.getReturnType().equals(Void.TYPE)) {
            generator.push((String)null);
        }

        // Pop the execution context
        frame.operandStack.pop();

        // Pop the arguments
        for(int i = 0; i < params.length; i++) {
            frame.operandStack.pop();
        }

        Class returnClass = null;
        if(isStatic) {
            // Push the return value
            if(method.getReturnType().equals(Void.TYPE)) {
                frame.operandStack.push(Object.class);
                returnClass = Object.class;
            } else {
                frame.operandStack.push(method.getReturnType());
                returnClass = method.getReturnType();
            }
        } else {
            // Pop the reciever then push the return value.
            frame.operandStack.pop();

            // Pop the duplicate of the reciever
            frame.operandStack.pop();

            // Push the return value
            // TODO: This should be Var eventually as specified by the
            // Function#apply method...
            frame.operandStack.push(Object.class);
            returnClass = Object.class;
        }

        Compiler.loadExecutionContext(context);
        generator.invokeVirtual(Type.getType(ExecutionContext.class), org.objectweb.asm.commons.Method.getMethod("int endCall()"));



        // ###
        // ### Post Call Site - Inspect the execution context to see if we need to pause or not

        Label continuationSite = generator.newLabel();

        Label running = generator.newLabel();
        Label resuming = generator.newLabel();
        Label capturing = generator.newLabel();
        Label yielding = generator.newLabel();
        Label rest = generator.newLabel();

        CompilationFrame.CallSite frameCallSite = new CompilationFrame.CallSite();
        frameCallSite.resumeSite = resumeSite;
        frameCallSite.continuationSite = continuationSite;
        frame.callSites.push(frameCallSite);
        programCounter = frame.callSites.size() - 1;

        generator.visitTableSwitchInsn(1, 4, running, new Label[] { running, resuming, capturing, yielding });

        generator.mark(running);
        if(!isStatic) {
            // Pop the duplicate reciever
            generator.swap(Type.getType(Function.class), Type.getType(returnClass));
            generator.pop();
        }
        generator.goTo(rest);

        generator.mark(resuming);
        // First, pop the duplicate reciever
        if(!isStatic) {
            // Pop the duplicate reciever
            generator.swap(Type.getType(Function.class), Type.getType(returnClass));
            generator.pop();
        }
        // Second, clear the stack
        for(int i = frame.operandStack.size() - 2; i >= 0; i--) {
            Class operandType = frame.operandStack.get(i);
            generator.swap(Type.getType(operandType), Type.getType(returnClass));
            Compiler.pop(operandType, generator);
        }
        // Box the return type to avoid any verification issues
        generator.box(Type.getType(returnClass));
        generator.checkCast(Type.getType(Object.class));
        // Restore the local variables
        generator.goTo(frame.restoreLocalsLabel);
        // I get transfered back here. Unbox the return type
        generator.mark(continuationSite);
        generator.unbox(Type.getType(returnClass));
        // Restore the stack
        for(int i = 0; i < frame.operandStack.size() - 1; i++) {
            Class operandType = frame.operandStack.get(i);

            // TODO: Is this more or less efficient than doing weird DUP / DUPX2 / Swaps
            Compiler.loadExecutionFrame(context);
            generator.getField(Type.getType(ExecutionFrame.class), "stack", Type.getType(Object[].class));
            generator.push(i);
            generator.arrayLoad(Type.getType(Object.class));

            generator.unbox(Type.getType(operandType));
            generator.swap(Type.getType(returnClass), Type.getType(operandType));
        }
        Compiler.loadExecutionContext(context);
        generator.push((String)null);
        generator.invokeVirtual(Type.getType(ExecutionContext.class), org.objectweb.asm.commons.Method.getMethod("void setCurrentFrame(silo.lang.ExecutionFrame)"));
        generator.goTo(rest);

        generator.mark(capturing);
        // Pop the return value, we do not care about it
        Compiler.pop(returnClass, generator);
        // Create new frame
        Compiler.loadExecutionContext(context);
        generator.newInstance(Type.getType(ExecutionFrame.class));
        generator.dup();
        generator.dup();
        generator.dup();
        // Call the frame constructor
        generator.invokeConstructor(Type.getType(ExecutionFrame.class), org.objectweb.asm.commons.Method.getMethod("void <init> ()"));
        // Set the program counter
        generator.push(programCounter);
        generator.putField(Type.getType(ExecutionFrame.class), "programCounter", Type.getType(int.class));
        // Set the operand stack - the size is the size of the operand stack - 1 because we ignore the return value
        if(isStatic) {
            generator.push(frame.operandStack.size() - 1);
        } else {
            // Extra room for the duplicated reciever
            generator.push(frame.operandStack.size() - 1 + 1);
        }
        generator.newArray(Type.getType(Object.class));
        generator.putField(Type.getType(ExecutionFrame.class), "stack", Type.getType(Object[].class));
        // Set the current frame
        generator.invokeVirtual(Type.getType(ExecutionContext.class), org.objectweb.asm.commons.Method.getMethod("void setCurrentFrame(silo.lang.ExecutionFrame)"));
        // Store the duplicated reciever
        if(!isStatic) {
            Compiler.loadExecutionFrame(context);
            generator.getField(Type.getType(ExecutionFrame.class), "stack", Type.getType(Object[].class));

            generator.swap(Type.getType(Function.class), Type.getType(Object[].class));
            generator.push(frame.operandStack.size() - 2 + 1);
            generator.swap(Type.getType(int.class), Type.getType(Object[].class));
            generator.arrayStore(Type.getType(Function.class));
        }
        // Store Stack - Skip the return value which is on the top
        for(int i = frame.operandStack.size() - 2; i >= 0; i--) {
            Class operandType = frame.operandStack.get(i);

            // TODO: Is this more or less efficient than doing weird DUP / DUPX2 / Swaps
            Compiler.loadExecutionFrame(context);
            generator.getField(Type.getType(ExecutionFrame.class), "stack", Type.getType(Object[].class));

            generator.swap(Type.getType(operandType), Type.getType(Object[].class));
            generator.push(i);
            generator.swap(Type.getType(int.class), Type.getType(Object[].class));
            generator.box(Type.getType(operandType));
            generator.arrayStore(Type.getType(Object.class));
        }
        // Store Local Variables
        generator.goTo(frame.captureLocalsLabel);

        generator.mark(yielding);
        // It turns out that I do not need to clear the stack before returning from a method.
        // Hence, I do not clear the returnValue nor (in the case of a function handle) the
        // receiver. The fact that I do not need to clear the stack is confirmed from tests
        // that I have run which inserts a bunch of "generator.push((String)null)" in this
        // code region as well as the JVM's online documentation where it seems to insinuate
        // that the stack is cleared:
        // http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html#jvms-6.5.areturn

        // Compiler.pop(returnClass, generator);
        // if(!staticInvoke) {
        //     Compiler.pop(Function.class, generator);
        // }
        Compiler.pushInitializationValue(frame.outputClass, generator);
        generator.returnValue();

        generator.mark(rest);
    }

    public Class type(CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        int size = frame.operandStack.size();

        emit(context, false);

        if(frame.operandStack.size() - size != 1) {
            System.out.println(frame.operandStack.size());
            System.out.println(frame.operandStack);
            throw new RuntimeException("Error!");
        }

        return frame.operandStack.pop();
    }

    public Object scaffold(CompilationContext context) {
        Object o = node;
        boolean didMacroExpansion = false;

        while(true) {
            if(o instanceof Node) {
                Node n = (Node)o;

                Object label = n.getLabel();
                Vector children = n.getChildren();

                Class klass = null;

                try {
                    klass = Compiler.resolveType(label, context);
                } catch(Exception e) {
                    klass = null;
                } catch(NoClassDefFoundError e) {
                    // TODO: This sometimes can generator a NoClassDefFoundError that can NOT be caught.
                    // The reproduce this errors, compile a program that uses `Node(null, null)`. Keep the
                    // CompilationContext imports the same as the commit where this comment was introduced.
                    klass = null;
                }

                if(Compiler.isMacro(klass)) {
                    CompilationContext.SymbolEntry entry = context.symbolTable.get(klass.getName());
                    if(entry != null && entry.compiled == false) {
                        context.namespaces.push(entry.namespace);
                        Compiler.buildExpression(entry.code).emit(context);
                        context.namespaces.pop();

                        klass = Compiler.resolveType(label, context);
                    }

                    o = context.runtime.eval(klass, children.toArray());
                    didMacroExpansion = true;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        if(o instanceof Node) {
            if(didMacroExpansion) {
                o = Compiler.buildExpression(o).scaffold(context);
            } else {
                o = Compiler.scaffoldNode((Node)o, context);
            }

            if(o instanceof Node) {
                ((Node)o).meta = node.meta;
            }

            return o;
        } else {
            return o;
        }
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

            if(!identifier.isEmpty() && frame.locals.containsKey(identifier.get(0))) {
                result = null;
            }

            if(result != null) {
                Class klass = (Class)result.get(0);
                Vector<Symbol> path = (Vector<Symbol>)result.get(1);

                if(path.size() == 0) {
                    // Native function or Constructor

                    if(Function.class.isAssignableFrom(klass)) {
                        // TODO: Pass Execution Context

                        // TODO: Should I support arity overloading?
                        // TODO: Should I support type overloading?
                        // TODO: If I do, I should consider doing it how Clojure does it... or implement it as a macro...

                        Method method = Function.methodHandle(klass);

                        if(!shouldEmit) {
                            // Fast exit to improve the performance of Expression#type()
                            frame.operandStack.push(method.getReturnType());
                            return;
                        }


                        Class[] params = method.getParameterTypes();
                        Vector<Class> types = argumentTypes(arguments, context);
                        params = Arrays.copyOfRange(params, 1, params.length);

                        boolean isVarArgs = Function.isVarArgs(klass);;

                        if(isVarArgs) {
                            if(types.size() < (params.length - 1)) {
                                throw new RuntimeException("Arity mismatch.");
                            }
                        } else {
                            if(types.size() != params.length) {
                                throw new RuntimeException("Arity mismatch.");
                            }
                        }

                        for(int i = 0; i < params.length; i++) {
                            if(isVarArgs && (i == (params.length - 1))) {
                                continue;
                            }

                            Class expectedType = params[i];
                            Class providedType = types.get(i);

                            if(Compiler.assignmentValidation(expectedType, providedType) != Compiler.AssignmentOperation.VALID) {
                                throw new RuntimeException("Parameter mismatch in " + klass + ". Expected: " + expectedType + " Provided: " + providedType);
                            }
                        }

                        Compiler.assertResumableContext(context);
                        performResumableInvoke(context, method, arguments);
                        return;
                    } else {
                        // TODO: Add another "else if" clause that checks for a "record" or "type"

                        // I no longer need these because I package up all constructor arguments.
                        //frame.operandStack.push(klass);
                        //frame.operandStack.push(klass);

                        Vector<Class> types = argumentTypes(arguments, context);

                        Constructor constructor = resolveConstructor(klass, types.toArray(new Class[0]));
                        if(constructor == null) {
                            throw new RuntimeException("Could not find constructor");
                        }

                        if(!shouldEmit) {
                            // Fast exit to improve the performance of Expression#type()
                            frame.operandStack.push(klass);
                            return;
                        }

                        Class[] params = constructor.getParameterTypes();
                        boolean shouldVarArgs = false;

                        if(constructor.isVarArgs()) {
                            shouldVarArgs = shouldUseVarArgs(params, types.toArray(new Class[0]));
                        }

                        if(shouldVarArgs) {
                            compileVariableArguments(params, arguments, context, shouldEmit);
                        } else {
                            compileArguments(arguments, context, shouldEmit);
                        }

                        if(shouldEmit) {
                            generator.push(params.length);
                            generator.newArray(Type.getType(Object.class));
                            generator.visitVarInsn(Type.getType(Object[].class).getOpcode(Opcodes.ISTORE), frame.locals.get(new Symbol("constructor:variable")));

                            for(int i = params.length - 1; i >= 0; i--) {
                                generator.visitVarInsn(Type.getType(Object[].class).getOpcode(Opcodes.ILOAD), frame.locals.get(new Symbol("constructor:variable")));
                                generator.swap(Type.getType(params[i]), Type.getType(Object[].class));

                                generator.push(i);
                                generator.swap(Type.getType(params[i]), Type.getType(int.class));

                                generator.box(Type.getType(params[i]));
                                generator.arrayStore(Type.getType(Object.class));
                            }

                            generator.newInstance(Type.getType(klass));
                            Compiler.dup(klass, generator);

                            for(int i = 0; i < params.length; i++) {
                                generator.visitVarInsn(Type.getType(Object[].class).getOpcode(Opcodes.ILOAD), frame.locals.get(new Symbol("constructor:variable")));
                                generator.push(i);
                                generator.arrayLoad(Type.getType(Object.class));

                                generator.unbox(Type.getType(params[i]));
                            }

                            generator.push((String)null);
                            generator.visitVarInsn(Type.getType(Object[].class).getOpcode(Opcodes.ISTORE), frame.locals.get(new Symbol("constructor:variable")));

                            generator.invokeConstructor(Type.getType(klass), org.objectweb.asm.commons.Method.getMethod(constructor));
                        }

                        // I no longer need this because I package up the args into an array
                        // Original Comment: Remove the class reference that was used to invoke the constructor
                        //frame.operandStack.pop();

                        for(int i = 0; i < params.length; i++) {
                            frame.operandStack.pop();
                        }

                        frame.operandStack.push(klass);

                        return;
                    }
                } else if(path.size() == 1) {
                    // Java static method

                    Symbol symbol = path.get(0);
                    Vector<Class> types = argumentTypes(arguments, context);

                    Method method = resolveMethod(klass, symbol.toString(), true, types.toArray(new Class[0]));
                    if(method == null) {
                        types.add(0, ExecutionContext.class);
                        method = resolveMethod(klass, symbol.toString(), true, types.toArray(new Class[0]));
                        if(method == null) {
                            throw new RuntimeException("Could not find function: " + symbol.toString());
                        }
                    }

                    if(!shouldEmit) {
                        // Fast exit to improve the performance of Expression#type()
                        frame.operandStack.push(method.getReturnType());
                        return;
                    }

                    if(Compiler.isResumableMethod(method)) {
                        Compiler.assertResumableContext(context);
                        performResumableInvoke(context, method, arguments);
                    } else {
                        performNonResumableInvoke(context, method, arguments);
                    }

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
            return;
        } else if(Function.class.isAssignableFrom(operand)) {

            // TODO: Right now I am forcing the use of apply which is an varargs method taking an Object[]
            Class[] argMask = new Class[arguments.size() + 1];
            argMask[0] = ExecutionContext.class;
            for(int i = 1; i < argMask.length; i++) {
                argMask[i] = Object.class;
            }

            Method method = resolveMethod(operand, "apply", false, argMask);

            if(method == null) {
                throw new RuntimeException("Could not find apply method with function handle.");
            }

            if(!shouldEmit) {
                frame.operandStack.push(method.getReturnType());
                return;
            }

            Compiler.assertResumableContext(context);
            performResumableInvoke(context, method, arguments);
            return;
        } else {
            // Dynamic function invocation
            // TODO: Remain cases...
            // TODO: Remember to throw exceptions if the symbol is legit not found.

            throw new RuntimeException("Dynamic invocation has not been implemented: " + operand);
        }
    }
}
