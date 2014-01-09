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

public class InvokeVirtual implements Expression {

    public final Expression receiver;
    public final Symbol method;
    public final Vector<Expression> arguments;

    public static InvokeVirtual build(Node node) {
        if(node.getChildren().size() != 2) {
            throw new RuntimeException("invokevirtual requires two arguments.");
        }

        Expression receiver = Compiler.buildExpression(node.getFirstChild());

        Vector<Expression> arguments = new Vector<Expression>();
        Object second = node.getSecondChild();

        if(second instanceof Node) {
            Node method = (Node)second;

            if(method.getLabel() instanceof Symbol) {
                if(method.getChildren() != null) {
                    for(Object child : method.getChildren()) {
                        arguments.add(Compiler.buildExpression(child));
                    }
                }

                return new InvokeVirtual(receiver, (Symbol)method.getLabel(), arguments);
            }
        }

        throw new RuntimeException("Error!");
    }

    public InvokeVirtual(Expression receiver, Symbol method, Vector<Expression> arguments) {
        this.receiver = receiver;
        this.method = method;
        this.arguments = arguments;
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


        if(shouldEmit) {
            receiver.emit(context);
        } else {
            frame.operandStack.push(receiver.type(context));
        }

        Class originalklass = frame.operandStack.peek();
        Class klass = originalklass;

        Vector<Class> types = Invoke.argumentTypes(arguments, context);

        java.lang.reflect.Method m = Invoke.resolveMethod(originalklass, method.toString(), false, types.toArray(new Class[0]));

        if(m == null && originalklass.isInterface()) {
            m = Invoke.resolveMethod(Object.class, method.toString(), false, types.toArray(new Class[0]));

            if(m != null) {
                klass = Object.class;
            }
        }

        if(m == null) {
            throw new RuntimeException("Could not find function: " + method.toString() + " in class: " + originalklass);
        }

        Class[] params = m.getParameterTypes();
        boolean shouldVarArgs = false;

        if(m.isVarArgs()) {
            shouldVarArgs = Invoke.shouldUseVarArgs(params, types.toArray(new Class[0]));
        }

        if(shouldVarArgs) {
            Invoke.compileVariableArguments(params, arguments, context, shouldEmit);
        } else {
            Invoke.compileArguments(arguments, context, shouldEmit);
        }

        if(shouldEmit) {
            if(klass.isInterface()) {
                generator.invokeInterface(Type.getType(klass), Method.getMethod(m));
            } else {
                generator.invokeVirtual(Type.getType(klass), Method.getMethod(m));
            }
        }

        // Pop the arguments
        for(int i = 0; i < params.length; i++) {
            frame.operandStack.pop();
        }

        // Pop the receiver
        frame.operandStack.pop();
        
        // Add the return type
        if(m.getReturnType().equals(Void.TYPE)) {
            if(shouldEmit) {
                generator.push((String)null);
            }

            frame.operandStack.push(Null.class);
        } else {
            frame.operandStack.push(m.getReturnType());
        }
    }
}
