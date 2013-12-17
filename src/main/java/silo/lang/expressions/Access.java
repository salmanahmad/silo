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

import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.GeneratorAdapter;

public class Access implements Expression {

    public final Expression head;
    public final Vector<Symbol> tail;

    public static Access build(Symbol symbol) {
        Vector<Symbol> tail = new Vector<Symbol>();
        tail.add(symbol);

        return new Access(null, tail);
    }

    public static Access build(Node node) {
        Node components = Node.flattenTree(node, new Symbol("."));

        Expression head = null;
        Vector<Symbol> tail = new Vector<Symbol>();

        for(int i = 0; i < components.getChildren().size(); i++) {
            Object component = components.getChildren().get(i);

            if(component instanceof Symbol) {
                tail.add((Symbol)component);
            } else {
                if(i == 0) {
                    head = Compiler.buildExpression(component);
                } else {
                    throw new RuntimeException("The '.' operator must take a symbol to look up. It was provided: " + component);
                }
            }
        }

        return new Access(head, tail);
    }

    public Access(Expression head, Vector<Symbol> tail) {
        this.head = head;
        this.tail = tail;
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        Class scope = null;
        boolean isStaticScope = true;

        Vector<Symbol> path;

        // TODO: Report importedPackages in the CompilationContext.
        // TODO: Abstract out the imported packages out of this scope here so the list can be used by others.
        // TODO: Probably make import a special form.
        Vector<String> importedPackages = new Vector<String>();
        importedPackages.add(""); // This is really important, actually...
        importedPackages.add("java.lang");
        importedPackages.add("java.util");
        importedPackages.add("java.io");
        importedPackages.add("silo.core");

        if(head != null) {
            head.emit(context);
            scope = frame.operandStack.peek();
            isStaticScope = false;

            path = tail;
        } else {
            // TODO: Handle imported variables

            if(frame.locals.containsKey(tail.get(0))) {
                int local = frame.locals.get(tail.get(0)).intValue();

                scope = frame.localTypes.get(tail.get(0));
                isStaticScope = false;

                generator.visitVarInsn(Type.getType(scope).getOpcode(Opcodes.ILOAD), local);
                frame.operandStack.push(scope);

                path = new Vector<Symbol>(tail);
                path.remove(0);
            } else {
                Vector result = Compiler.resolveAccessPath(tail, importedPackages, loader);

                if(result == null) {
                    throw new RuntimeException("Could not find symbol: " + tail.toString());
                }

                scope = (Class)result.get(0);
                isStaticScope = true;

                path = (Vector<Symbol>)result.get(1);
            }
        }

        for(Symbol symbol : path) {
            try {
                java.lang.reflect.Field field = scope.getField(symbol.toString());

                if(isStaticScope) {
                    generator.getStatic(Type.getType(scope), field.getName(), Type.getType(field.getType()));
                    frame.operandStack.push(field.getType());
                    isStaticScope = false;
                } else {
                    Class operand = frame.operandStack.pop();
                    generator.getField(Type.getType(operand), field.getName(), Type.getType(field.getType()));
                    frame.operandStack.push(field.getType());
                }
            } catch(NoSuchFieldException e) {
                throw new RuntimeException("No such field was found: " + symbol.toString());
            }

            // TODO: What about obscured packages? Do they stay obscured? In other words, suppose there are two class
            // foo.bar.Baz.a.Car and foo.bar.Baz. If the class "Baz" does not have a field "a" the current implementation
            // will throw an exception, it will not attempt to find package "foo.bar.Baz.a".
        }
    }
}
