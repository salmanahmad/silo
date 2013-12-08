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
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.util.*;


// TODO: Should I rename all of the other expressions so that they have the "Expression" suffix?

public class FunctionExpression implements Expression, Opcodes {

    Symbol name;
    Vector inputs;
    Vector outputs;
    Block body;

    public static FunctionExpression build(Node node) {
        Symbol name = null;
        Vector inputs = null;
        Vector outputs = null;
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

    public void emit(CompilationContext context) {

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

        Vector<Type> inputTypes = new Vector<Type>();
        for(Object o : inputs) {
            // TODO: Add these inputs to the local variables

            if(o instanceof Symbol) {
                // TODO: Change this to "Var"
                inputTypes.add(Type.INT_TYPE);
            } else if(o instanceof Node) {
                Node node = (Node)o;

                // TODO: What about generics or arrays or scoped types?
                Symbol symbol = (Symbol)node.getSecondChild();
                Class klass = Compiler.primitives.get(symbol);

                if(klass == null) {
                    throw new RuntimeException("Only primitives are supported right now.");
                } else {
                    inputTypes.add(Type.getType(klass));
                }
            } else {
                throw new RuntimeException("Invalid input specification for function: " + o);
            }
        }

        Type outputType = Type.VOID_TYPE;
        if(outputs.size() == 1) {
            Symbol symbol = (Symbol)outputs.get(0);
            Class klass = Compiler.primitives.get(symbol);

            if(klass == null) {
                throw new RuntimeException("Only primitives are supported right now.");
            } else {
                outputType = Type.getType(klass);
            }
        }

        GeneratorAdapter g;
        AnnotationVisitor av;
        Method m;

        // The function class definition
        // TODO - Figure out how to propagate the file name
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visitSource("app", null);
        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, name.toString(), null, Type.getType(Function.class).getInternalName(), null);
        av = cw.visitAnnotation(Type.getType(Function.Definition.class).getDescriptor(), true);
        av.visitEnd();

        // Default constructor
        m = Method.getMethod("void <init> ()");
        g = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cw);
        g.loadThis();
        g.invokeConstructor(Type.getType(Function.class), m);
        g.returnValue();
        g.endMethod();

        // Static invoke method
        m = new Method("invoke", outputType, inputTypes.toArray(new Type[0]));
        g = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, cw);
        av = g.visitAnnotation(Type.getType(Function.Body.class).getDescriptor(), true);
        av.visitEnd();

        context.frames.push(new CompilationFrame(g));
        body.emit(context);
        context.frames.pop();

        g.returnValue();
        g.endMethod();

        cw.visitEnd();

        byte[] code = cw.toByteArray();
        Class klass = context.runtime.loader.loadClass(code);
        context.classes.add(klass);
    }
}
