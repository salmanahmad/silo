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

public class InvokeSpecial implements Expression {

    Node node;

    Expression receiver;
    Symbol method;
    Vector<Expression> arguments;

    public InvokeSpecial(Node node) {
        this.node = node;
    }

    public void validate() {
        if(node.getChildren().size() != 3) {
            throw new RuntimeException("invokespecial requires three arguments.");
        }

        receiver = Compiler.buildExpression(node.getFirstChild());
        arguments = new Vector<Expression>();

        Object second = node.getLastChild();
        if(second instanceof Node) {
            Node methodNode = (Node)second;

            if(methodNode.getLabel() instanceof Symbol) {
                if(methodNode.getChildren() != null) {
                    for(Object child : methodNode.getChildren()) {
                        arguments.add(Compiler.buildExpression(child));
                    }
                }

                method = (Symbol)methodNode.getLabel();
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
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        emit(context, true);
    }

    private void emit(CompilationContext context, boolean shouldEmit) {
        validate();

        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        if(shouldEmit) {
            receiver.emit(context);
        } else {
            // I actually need this here, even for fast exit for Expression#type()
            // because I use frame.operandStack.peek() to determine the method.
            frame.operandStack.push(receiver.type(context));
        }

        //Class originalklass = frame.operandStack.peek();
        Class originalklass = Compiler.resolveType(node.getSecondChild(), context);

        Vector<Class> types = Invoke.argumentTypes(arguments, context);

        java.lang.reflect.Method m = Invoke.resolveMethod(originalklass, method.toString(), false, types.toArray(new Class[0]));

        if(m == null) {
            throw new RuntimeException("Could not find function: " + method.toString() + " in class: " + originalklass);
        }

        if(!shouldEmit) {
            // Fast exit to improve the performance of Expression#type()
            // But first, pop the reciever type that we pushed.
            frame.operandStack.pop();
            frame.operandStack.push(m.getReturnType());
            return;
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

        generator.invokeConstructor(Type.getType(originalklass), Method.getMethod(m));

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
