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

package silo.lang.compiler;

import silo.lang.Runtime;
import silo.lang.*;
import silo.lang.expressions.*;

import java.util.Vector;
import java.util.HashMap;

import org.objectweb.asm.commons.GeneratorAdapter;

// TODO: Consider moving Compiler and Parser out of the 'compiler' package and simply into the 'lang' package instead.

public class Compiler {

    public static HashMap<Symbol, Class> primitives = new HashMap<Symbol, Class>();
    static {
        primitives.put(new Symbol("boolean"), Boolean.TYPE);
        primitives.put(new Symbol("char"), Character.TYPE);
        primitives.put(new Symbol("byte"), Byte.TYPE);
        primitives.put(new Symbol("short"), Short.TYPE);
        primitives.put(new Symbol("int"), Integer.TYPE);
        primitives.put(new Symbol("long"), Long.TYPE);
        primitives.put(new Symbol("float"), Float.TYPE);
        primitives.put(new Symbol("double"), Double.TYPE);
        primitives.put(new Symbol("void"), Void.TYPE);
    }

    public static Vector<Class> compile(CompilationContext context, Node node) {
        Expression expression = buildExpression(node);
        expression.emit(context);

        return context.classes;

        /*
        node = expandMacros(node);

        Vector<Symbol> declarations = extractDeclarations(node);
        checkForDuplicates(declarations);

        Vector<Node> containsUncompiledMacros(node)



        Object expressionTree = transformToExpressions(node);

        emit(expressionTree);
        */
    }

    public static Object expandMacros(Object node) {
        /*
        Object previous = null;

        while(true) {
            previous = node;
            node = expandMacrosOnce(node);

            if(previous.equals(node)) {
                break;
            }
        }

        return node;
        */
        return null;
    }

    private static void checkForDuplicates(Vector<Node> declarations) {
        
    }

    public static Object expandMacrosOnce(Object node) {
        // TODO
        return null;
    }

    private static boolean containsUncompiledMacros(Object value) {
        return false;
    }

    public static Vector<Node> extractDeclarations(Node node) {
        return null;
    }

    public static Expression buildExpression(Object value) {
        if(value instanceof Node) {
            Node node = (Node)value;
            Object label = node.getLabel();

            // TODO: Create an "ExpressionBuilder" interface that all the Expressions have
            // as a nested class. Then, just create a list of "ExpressionBuilder" as "Special-Forms"
            // and literate over them instead of having this huge if-else statement.

            if(node.getLabel() == null) {
                return Block.build(node);
            } else if(label.equals(new Symbol("do"))) {
                return Block.build(node);
            } else if(label.equals(new Symbol("function"))) {
                return FunctionExpression.build(node);
            } else if(label.equals(new Symbol("declare"))) {
                return Declare.build(node);
            } else if(label.equals(new Symbol("return"))) {
                return Return.build(node);
            } else if(label.equals(new Symbol("invokevirtual"))) {
                // TODO: Add macro called "dispatch" to wrap this...
                return InvokeVirtual.build(node);
            } else if(label.equals(new Symbol("."))) {
                return Access.build(node);
            } else if(MathOperation.accepts(node.getLabel())) {
                return MathOperation.build(node);
            } else {
                return Invoke.build(node);
            }
        } else if(value instanceof Integer) {
            int i = ((Integer)value).intValue();
            return new LiteralInteger(i);
        } else if(value instanceof Symbol) {
            if(value.equals(new Symbol("return"))) {
                return Return.build(new Node(new Symbol("return")));
            } else if(value.equals(new Symbol("break"))) {
                // TODO
            } else if(value.equals(new Symbol("continue"))) {
                // TODO
            }

            return Access.build((Symbol)value);
        } else {
            throw new RuntimeException("Unhandled case..." + value.toString());
        }
    }

    public static void emit(Node node) {
        
    }

    public static boolean isCategory2(Class klass) {
        return klass.equals(Double.TYPE) || klass.equals(Long.TYPE);
    }

    public static void pop(Class klass, GeneratorAdapter generator) {
        if(isCategory2(klass)) {
            generator.pop2();
        } else if(!klass.equals(Void.TYPE)) {
            generator.pop();
        }
    }

}



