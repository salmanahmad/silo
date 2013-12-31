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

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        receiver.emit(context);
        Class klass = frame.operandStack.peek();

        Vector<Class> types = Invoke.compileArguments(arguments, context);

        java.lang.reflect.Method m = Invoke.getMethod(klass, method.toString(), false, types.toArray(new Class[0]));

        if(m == null) {
            throw new RuntimeException("Could not find function: " + method.toString() + " in class: " + klass);
        }

        generator.invokeVirtual(Type.getType(klass), Method.getMethod(m));

        // Pop the arguments
        for(Expression e : arguments) {
            frame.operandStack.pop();
        }

        // Pop the receiver
        frame.operandStack.pop();
        
        // Add the return type
        if(!m.getReturnType().equals(Void.TYPE)) {
            // TODO: Return null if it is a void return.
            frame.operandStack.push(m.getReturnType());
        }
    }
}
