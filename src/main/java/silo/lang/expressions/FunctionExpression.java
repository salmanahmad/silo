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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.PrintStream;


// TODO: Should I rename all of the other expressions so that they have the "Expression" suffix?
// TODO: Should I call this a LiteralFunction? Or should I call LiteralArray ArrayExpression and ArrayTypeExpression?

public class FunctionExpression implements Expression, Opcodes {

    Symbol name;
    Vector inputs;
    Vector outputs;
    Block body;
    boolean macro;

    public static FunctionExpression build(Node node) {
        Symbol name = null;
        Vector inputs = null;
        Vector outputs = null;
        Block body = null;
        boolean macro = false;

        Node n = null;
        Object o = null;


        o = node.getChildNamed(new Symbol("macro"));
        macro = o != null;

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

        return new FunctionExpression(name, inputs, outputs, body, macro);
    }

    public FunctionExpression(Symbol name, Vector inputs, Vector outputs, Block body, boolean macro) {
        this.name = name;
        this.inputs = inputs;
        this.outputs = outputs;
        this.body = body;
        this.macro = macro;
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
            if(o instanceof Symbol) {
            } else if(o instanceof Node) {
            } else {
                throw new RuntimeException("Invalid output specification. Must be an identifier.");
            }
        }

        Vector<Type> inputTypes = new Vector<Type>();
        Vector<Class> inputClasses = new Vector<Class>();
        Vector<Symbol> inputNames = new Vector<Symbol>();
        for(Object o : inputs) {
            // TODO: Add these inputs to the local variables

            if(o instanceof Symbol) {
                // This is the case where the user did not supply any type information

                // TODO: Change this to "Var"
                inputTypes.add(Type.getType(Object.class));
                inputClasses.add(Object.class);
                inputNames.add((Symbol)o);
            } else if(o instanceof Node) {
                Node node = (Node)o;

                // TODO: What about generics or arrays?
                Symbol name = (Symbol)node.getFirstChild();
                Object symbol = node.getSecondChild();

                Class klass = Compiler.resolveType(symbol, context);

                if(klass == null) {
                    throw new RuntimeException("Could not find symbol: " + symbol.toString());
                }

                inputTypes.add(Type.getType(klass));
                inputClasses.add(klass);
                inputNames.add(name);
            } else {
                throw new RuntimeException("Invalid input specification for function: " + o);
            }
        }

        Type outputType = Type.VOID_TYPE;
        Class outputClass = Void.TYPE;
        if(outputs.size() == 1) {
            // TODO: Add special case for "null" as well...
            // TODO: Handle non-primitive types and dot expressions...
            // TODO: Multiple outputs?
            // TODO: What about generics or arrays?

            Object symbol = outputs.get(0);
            Class klass = Compiler.resolveType(symbol, context);

            if(klass == null) {
                throw new RuntimeException("Could not find symbol: " + symbol.toString());
            }

            outputType = Type.getType(klass);
            outputClass = klass;
        }

        if(outputType.equals(Type.VOID_TYPE)) {
            // TODO: make this var when you implement vars and traits
            outputType = Type.getType(Object.class);
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
        if(macro) {
            av.visit("macro", Boolean.TRUE);
        }
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

        CompilationFrame frame = new CompilationFrame(ACC_PUBLIC + ACC_STATIC, m, g, outputClass);

        context.frames.push(frame);

        if(inputClasses.size() == inputNames.size()) {
            for(int i = 0; i < inputClasses.size(); i++) {
                frame.newLocal(inputNames.get(i), inputClasses.get(i));
            }
        } else {
            throw new RuntimeException("Internal error. The length of the input names and types should be the same.");
        }

        body.emit(context);
        (new Return(null, false)).emit(context);
        context.frames.pop();

        //g.returnValue();
        g.endMethod();

        cw.visitEnd();

        byte[] code = cw.toByteArray();

        /*
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ClassReader classReader = new ClassReader(code);
        PrintWriter printWriter = new PrintWriter(outputStream);
        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(printWriter);
        classReader.accept(traceClassVisitor, ClassReader.SKIP_DEBUG);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        CheckClassAdapter.verify(new ClassReader(code), false, pw);

        System.out.println(sw.toString());

        System.out.println();
        System.out.println(outputStream.toString());
        System.out.println();
        System.out.println();
        */

        Class klass = context.runtime.loader.loadClass(code);
        context.classes.add(klass);
        context.bytecode.add(code);
    }
}
