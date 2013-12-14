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

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        if(receiver == null) {
            // TODO: Handle local and imported variables

            // TODO: Report importedPackages in the CompilationContext.
            // TODO: Abstract out the imported packages out of this scope here so the list can be used by others.
            // TODO: Probably make import a special form.
            Vector<String> importedPackages = new Vector<String>();
            importedPackages.add(""); // This is really important, actually...
            importedPackages.add("java.lang");
            importedPackages.add("java.util");
            importedPackages.add("java.io");

            Vector result = Access.classAtPath(identifier, importedPackages, loader);

            if(result != null) {
                Class klass = (Class)result.get(0);
                Vector<Symbol> path = (Vector<Symbol>)result.get(1);

                if(path.size() == 0) {
                    // Native function
                    // TODO
                } else if(path.size() == 1) {
                    // Java static method

                    Symbol symbol = path.get(0);
                    Vector<Class> types = compileArguments(arguments, context);

                    java.lang.reflect.Method method = null;

                    try {
                        method = klass.getMethod(symbol.toString(), types.toArray(new Class[0]));
                    } catch(NoSuchMethodException e) {
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

        return;
    }
}
