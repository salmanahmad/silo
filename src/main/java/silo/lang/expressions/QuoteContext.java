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
import org.objectweb.asm.commons.GeneratorAdapter;

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

public class QuoteContext implements Expression {

    public static final Symbol DOT = new Symbol(".");

    Node node;

    public QuoteContext(Node node) {
        this.node = node;
    }

    public static Object qualifiedClassName(Class klass) {
        String[] parts = klass.getName().split("\\.");

        if(parts.length == 1) {
            return new Symbol(parts[0]);
        } else {
            Node node = new Node(
                DOT,
                new Symbol(parts[0]),
                new Symbol(parts[1])
            );

            for(int i = 2; i < parts.length; i++) {
                node = new Node(
                    DOT,
                    node,
                    new Symbol(parts[i])
                );
            }

            return node;
        }
    }

    public static void quote(Object value, CompilationContext context) {
        if(value instanceof Node) {
            Node node = (Node)value;

            Vector children = node.getChildren();
            Object label = node.getLabel();

            if(DOT.equals(label)) {
                Vector<Symbol> identifier = Node.symbolListFromNode(Node.flattenTree(node, DOT));

                if(identifier != null) {
                    Vector result = Compiler.resolveIdentifierPath(identifier, context);
                    if(result != null) {
                        node = (Node)qualifiedClassName((Class)result.get(0));
                        Vector<Symbol> list = (Vector<Symbol>)result.get(1);
                        for(Symbol s : list) {
                            node = new Node(
                                DOT,
                                node,
                                s
                            );
                        }
                    }
                }
            }

            quoteNode(node, context);
        } else if(value instanceof Symbol) {
            Class klass = Compiler.resolveType(value, context);
            if(klass != null) {
                Object name = qualifiedClassName(klass);
                if(!name.equals(value)) {
                    quote(name, context);
                    return;
                }
            }

            quoteSymbol((Symbol)value, context);
        } else {
            // Nulls and Other Literals
            // TODO: Auto-Boxing
            Compiler.buildExpression(value).emit(context);
        }
    }

    public static void quoteSymbol(Symbol symbol, CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = context.currentFrame().generator;

        generator.push(symbol.name);
        generator.invokeStatic(
            Type.getType(Symbol.class),
            new org.objectweb.asm.commons.Method(
                "create",
                Type.getType(Symbol.class),
                new Type[] { Type.getType(String.class) }
            )
        );
        frame.operandStack.push(Symbol.class);
    }

    public static void quoteNode(Node node, CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = context.currentFrame().generator;

        Vector children = node.getChildren();
        Object label = node.getLabel();



        // Create a temporary vector
        generator.invokeStatic(
            Type.getType(PersistentVector.class),
            new org.objectweb.asm.commons.Method(
                "emptyVector",
                Type.getType(PersistentVector.class),
                new Type[0]
            )
        );
        frame.operandStack.push(PersistentVector.class);



        // Quote the label
        quote(label, context);
        generator.invokeVirtual(
            Type.getType(PersistentVector.class),
            new org.objectweb.asm.commons.Method(
                "cons",
                Type.getType(PersistentVector.class),
                new Type[] { Type.getType(Object.class) }
            )
        );
        frame.operandStack.pop();



        // Quote the children
        for(int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            if(DOT.equals(label) && (child instanceof Symbol)) {
                quoteSymbol((Symbol)child, context);
            } else {
                quote(child, context);
            }

            generator.invokeVirtual(
                Type.getType(PersistentVector.class),
                new org.objectweb.asm.commons.Method(
                    "cons",
                    Type.getType(PersistentVector.class),
                    new Type[] { Type.getType(Object.class) }
                )
            );
            frame.operandStack.pop();
        }



        // Create the actual node
        generator.invokeStatic(
            Type.getType(Node.class),
            new org.objectweb.asm.commons.Method(
                "fromVector",
                Type.getType(Node.class),
                new Type[] { Type.getType(IPersistentVector.class) }
            )
        );
        frame.operandStack.pop();
        frame.operandStack.push(Node.class);
    }

    public Class type(CompilationContext context) {
        return Object.class;
    }

    public Node scaffoldNode(Node n, CompilationContext context) {
        Object scaffoldedLabel = n.getLabel();
        if(scaffoldedLabel instanceof Node) {
            scaffoldedLabel = scaffoldNode((Node)scaffoldedLabel, context);
        }

        Node scaffoldedNode = Node.withMeta(n.getMeta(), scaffoldedLabel);
        Vector children = n.getChildren();

        if((new Symbol("escape")).equals(scaffoldedLabel)) {
            if(children.size() != 1) {
                throw new RuntimeException("escape requires a single argument");
            }

            scaffoldedNode.addChild(Compiler.buildExpression(children.get(0)).scaffold(context));
            return scaffoldedNode;
        }

        for(Object child : children) {
            if(child instanceof Node) {
                Node childNode = (Node)child;
                scaffoldedNode.addChild(scaffoldNode(childNode, context));
            } else {
                scaffoldedNode.addChild(child);
            }
        }

        return scaffoldedNode;
    }

    public Object scaffold(CompilationContext context) {
        return scaffoldNode(node, context);
    }

    public void emit(CompilationContext context) {
        Vector children = node.getChildren();
        if(children.size() != 1) {
            throw new RuntimeException("quotecontext requires a single argument");
        }

        quote(children.get(0), context);
    }
}
