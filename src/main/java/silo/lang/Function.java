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

package silo.lang;

import java.util.Arrays;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

public class Function {

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Definition {
        boolean macro() default false;
        boolean varargs() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Body {
    }

    Method methodHandle;
    Object[] resumptionArgs;

    public String getName() {
        throw new RuntimeException("Unimplemented");
    }

    public static boolean isVarArgs(Class klass) {
        if(klass.isAnnotationPresent(Definition.class)) {
            Definition d = (Definition)klass.getAnnotation(Definition.class);
            return d.varargs();
        }

        return false;
    }

    public static Object[] convertArgsToVarArgs(Class klass, Object... args) {
        Method handle = methodHandle(klass);

        int size = handle.getParameterTypes().length;
        Object[] varargs = new Object[size];
        IPersistentVector vec = PersistentVector.emptyVector();

        for(int i = 0; i < args.length; i++) {
            if(i < (size - 1)) {
                varargs[i] = args[i];
            } else {
                vec = vec.cons(args[i]);
            }
        }

        varargs[size - 1] = vec;
        return varargs;
    }

    public static Method methodHandle(Class klass) {
        Method[] methods = klass.getMethods();
        for(Method method : methods) {
            if(method.getAnnotation(Body.class) != null) {
                return method;
            }
        }

        throw new RuntimeException("Could not find method handle");
    }

    public Method methodHandle() {
        if(methodHandle == null) {
            methodHandle = methodHandle(this.getClass());
        }

        return methodHandle;
    }

    /*
    // TODO: Emit a highly efficient version of invoke in FunctionExpression that does not rely
    // on java refliection method handles which are really slow...

    // TODO: Invoke should be broken up into a bunch of overloaded invoke methods
    // so that it can be dispatch by airity very efficiently without having to go through the
    // Java reflection API which is slow

    public Object invoke(Object[] args, ExecutionContext context) {
        if(args == null) {
            args = new Object[0];
        }

        args = Arrays.copyOf(args, args.length + 1);
        args[args.length - 1] = context;

        Method method = methodHandle();

        try {
             return method.invoke(null, args);
        } catch(Exception e) {
            throw new RuntimeException("Error occurred calling a method.");
        }
    }

    public Object resume(ExecutionContext context) {
        if(resumptionArgs == null) {
            Method method = this.methodHandle();

            // I use argTypes.length - 1 because I am passing in ExecutionContext explicitly
            Class[] argTypes = method.getParameterTypes();
            Object[] args = new Object[argTypes.length - 1];

            for(int i = 0; i < args.length - 1; i++) {
                Class argType = argTypes[i];
                if(argType.isPrimitive()) {
                    if(argType.equals(Byte.TYPE)) {
                        args[i] = new Byte((byte)0);
                    } else if(argType.equals(Short.TYPE)) {
                        args[i] = new Short((short)0);
                    } else if(argType.equals(Integer.TYPE)) {
                        args[i] = new Integer(0);
                    } else if(argType.equals(Long.TYPE)) {
                        args[i] = new Long(0L);
                    } else if(argType.equals(Float.TYPE)) {
                        args[i] = new Float(0f);
                    } else if(argType.equals(Double.TYPE)) {
                        args[i] = new Double(0d);
                    } else if(argType.equals(Character.TYPE)) {
                        args[i] = new Character('c');
                    } else if(argType.equals(Boolean.TYPE)) {
                        args[i] = new Boolean(false);
                    }
                } else {
                    args[i] = null;
                }
            }

            this.resumptionArgs = args;
        }

        return this.invoke(resumptionArgs, context);
    }
    */

    public Object apply(Object... args) {
        try {
            return Runtime.doEval(this.getClass(), args);
        } catch(Exception e) {
            throw new RuntimeException("Error occurred calling a method.");
        }
    }

    // TODO: Enable these again...
    /*
    public Object apply() {
        return arityException(0);
    }

    public Object apply(Object o1) {
        return arityException(1);
    }

    public Object apply(Object o1, Object o2) {
        return arityException(2);
    }

    public Object apply(Object o1, Object o2, Object o3) {
        return arityException(3);
    }

    public Object apply(Object o1, Object o2, Object o3, Object o4) {
        return arityException(4);
    }

    public Object apply(Object o1, Object o2, Object o3, Object o4, Object o5) {
        return arityException(5);
    }

    public Object apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
        return arityException(6);
    }

    public Object apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
        return arityException(7);
    }

    public Object apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
        return arityException(8);
    }

    public Object apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
        return arityException(9);
    }

    public Object apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10) {
        return arityException(10);
    }

    public Object apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10, Object... o11) {
        return arityException(10 + o11.length);
    }

    public Object arityException(int length) {
        throw new RuntimeException("Arity exception. Function does not take " + length + " arguments");
    }
    */
}