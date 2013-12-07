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

// TODO: Should I rename all of the other expressions so that they have the "Expression" suffix?

public class FunctionExpression implements Expression {

    Symbol name;
    Vector<Node> inputs;
    Vector<Node> outputs;
    Block body;

    public static FunctionExpression build(Node node) {
        Symbol name = null;
        Vector<Node> inputs = null;
        Vector<Node> outputs = null;
        Block body = null;

        Node n = null;
        Object o = null;

        n = node.getChildNode(new Symbol("name"));
        if(n != null) {
            o = n.getFirstChild();
            if(o instanceof Symbol) {
                name = (Symbol)o;
            } else {
                throw new RuntimeException("The name of a function must be a symbol.");
            }
        }

        n = node.getChildNode(new Symbol("inputs"));
        if(n != null) {
            inputs = n.getChildren();
        }

        n = node.getChildNode(new Symbol("outputs"));
        if(n != null) {
            outputs = n.getChildren();
        }

        n = node.getChildNode(new Symbol("do"));
        if(n != null) {
            body = Block.build(n);
        }

        return new FunctionExpression(name, inputs, outputs, body);
    }

    public FunctionExpression(Symbol name, Vector inputs, Vector outputs, Block body) {
        this.name = name;
        this.inputs = inputs;
        this.outputs = outputs;
        this.body = body;
    }

    public void emit(CompilationContext context, GeneratorAdapter generator) {

        if(name == null) {
            name = context.uniqueIdentifier("function");
        }

        if(outputs == null) {
            outputs = new Vector();
        }

        if(inputs == null) {
            inputs = new Vector();
        }

        if(body == null) {
            body = new Block(null);
        }

        if(outputs.size() > 1) {
            throw new RuntimeException("Multiple output values is not supported");
        }

        for(Object o : outputs) {
            if(!(o instanceof Symbol)) {
                // TODO: What about generics or arrays or scoped types?
                throw new RuntimeException("Named output values is not supported. All outputs must be a symbol representing a type reference");
            }
        }

        

    }
}
