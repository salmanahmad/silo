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

// TODO: Rename this to dispatch? I am not doing JVM virtual-calls all the time...

public class InvokeVirtual implements Expression {

    Node node;

    // TODO: Figure out how to remove this...
    // As an idea...right now I have these fields to share them between emitDeclaration and emit.
    // What if I have a method call "validate" which throws an exception if it is of a improper form.
    // Then I can unpack / use pattern matching in both of these methods at will...
    Expression receiver;
    Symbol method;
    Vector<Expression> arguments;

    public InvokeVirtual(Node node) {
        this.node = node;
    }

    public void validate() {
        if(node.getChildren().size() != 2) {
            throw new RuntimeException("invokevirtual requires two arguments.");
        }

        receiver = Compiler.buildExpression(node.getFirstChild());
        arguments = new Vector<Expression>();

        Object second = node.getSecondChild();
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

        Class originalklass = frame.operandStack.peek();
        Class klass = originalklass;

        Vector<Class> types = Invoke.argumentTypes(arguments, context);
        java.lang.reflect.Method m = Invoke.resolveMethod(originalklass, method.toString(), false, types.toArray(new Class[0]));

        if(m == null) {
            Vector<Class> resumableTypes = (Vector<Class>)types.clone();
            resumableTypes.add(0, ExecutionContext.class);
            m = Invoke.resolveMethod(originalklass, method.toString(), false, resumableTypes.toArray(new Class[0]));

            if(m == null && originalklass.isInterface()) {
                m = Invoke.resolveMethod(Object.class, method.toString(), false, types.toArray(new Class[0]));

                if(m != null) {
                    klass = Object.class;
                }
            } else {
                types = resumableTypes;
            }
        }

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

        if(Compiler.isResumableMethod(m)) {
            Compiler.assertResumableContext(context);
            Invoke.performResumableInvoke(context, m, arguments);
        } else {
            Invoke.performNonResumableInvoke(context, m, arguments);
        }
    }
}
