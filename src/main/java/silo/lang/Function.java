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

import silo.util.Helper;

import java.util.Arrays;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Function {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface FunctionDefinition {
        String name();
        String[] params() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface FunctionBody {

    }

    Method methodHandle;
    Object[] resumptionArgs;

    public String getName() {
        throw new RuntimeException("Unimplemented");
    }

    public Method methodHandle() {
        if(methodHandle == null) {
            Method[] methods = this.getClass().getMethods();
            for(Method method : methods) {
                if(method.getAnnotation(FunctionBody.class) != null) {
                    methodHandle = method;
                    break;
                }
            }

            if(methodHandle == null) {
                throw new RuntimeException("Could not find method handle");
            }
        }

        return methodHandle;
    }

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
}