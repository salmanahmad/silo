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

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.util.*;





import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.PrintStream;





public class Compiler implements Opcodes {

    public static Vector<Node> compile(Runtime runtime, Node node) {
        
        System.out.println(node);
        
        Expression expression = buildExpression(node);

        CompilationContext context = new CompilationContext(runtime);
        GeneratorAdapter generator = null;

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_1, ACC_PUBLIC, "Example", null, "java/lang/Object", null);

        Method m = Method.getMethod("void <init> ()");
        generator = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cw);
        generator.loadThis();
        generator.invokeConstructor(Type.getType(Object.class), m);
        generator.returnValue();
        generator.endMethod();

        m = Method.getMethod("void main ()");
        generator = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, cw);
        expression.emit(context, generator);
        generator.returnValue();
        generator.endMethod();

        cw.visitEnd();

        byte[] code = cw.toByteArray();
        Class klass = runtime.loader().loadClass(code);






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









        try {
            klass.getMethod("main").invoke(null);
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error!");
        }
        

        return null;

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

            if(node.getLabel() == null) {
                return Block.build(node);
            } else if(label.equals(new Symbol("do"))) {
                return Block.build(node);
            } else if(label.equals(new Symbol("function"))) {
                return FunctionExpression.build(node);
            } else if(label.equals(new Symbol("declare"))) {
                return Declare.build(node);
            } else if(MathOperation.accepts(node.getLabel())) {
                return MathOperation.build(node);
            } else {
                return Invoke.build(node);
            }

        } else if(value instanceof Integer) {
            int i = ((Integer)value).intValue();
            return new LiteralInteger(i);
        } else {
            throw new RuntimeException("Unhandled case...");
        }
    }

    public static void emit(Node node) {
        
    }
}



