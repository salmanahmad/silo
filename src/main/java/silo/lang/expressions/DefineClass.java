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

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.IPersistentMap;

import org.objectweb.asm.Type;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.util.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.PrintStream;

public class DefineClass implements Expression, Opcodes {

    Node node;

    public DefineClass(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Class.class;
    }

    public Object scaffoldMethod(Node node, CompilationContext context) {
        Vector children = new Vector();
        for(Object child : node.getChildren()) {
            if(child instanceof Node) {
                Node childNode = (Node)child;
                if(childNode.getLabel() == null) {
                    children.add(Compiler.buildExpression(childNode).scaffold(context));
                } else {
                    children.add(child);
                }
            } else {
                children.add(child);
            }
        }

        Node scaffolded = new Node(node.getLabel(), children);
        scaffolded.meta = node.meta;

        return scaffolded;
    }

    public Object scaffold(CompilationContext context) {
        Vector children = new Vector();
        for(Object child : node.getChildren()) {
            if(child instanceof Node) {
                Node childNode = (Node)child;
                if(new Symbol("method").equals(childNode.getLabel())) {
                    children.add(scaffoldMethod(childNode, context));
                } else {
                    children.add(child);
                }
            } else {
                children.add(child);
            }
        }

        Node scaffolded = new Node(node.getLabel(), children);
        scaffolded.meta = node.meta;

        doEmit(scaffolded, context, false);

        return scaffolded;
    }

    public void emit(CompilationContext context) {
        doEmit(node, context, true);
    }

    public static Symbol getSymbol(Node node, String symbol) {
        Symbol s = new Symbol(symbol);

        Node n = node.getChildNode(s);
        if(n != null) {
            if(n.getFirstChild() instanceof Symbol) {
                return (Symbol)n.getFirstChild();
            }
        }

        return null;
    }

    public static Object getObject(Node node, String symbol) {
        Symbol s = new Symbol(symbol);

        Node n = node.getChildNode(s);
        if(n != null) {
            return n.getFirstChild();
        }

        return null;
    }

    public IPersistentMap doEmitMethod(Node node, CompilationContext context, IPersistentMap methods, ClassWriter cw, String fullyQualifiedName, boolean shouldEmit) {
        /*method(
            name(i)
            modifiers(static, public)
            inputs(int)
            outputs(int) {
                ...
            }
        )*/

        Symbol name = getSymbol(node, "name");
        Node modifiersNode = node.getChildNode(new Symbol("modifiers"));
        Node inputsNode = node.getChildNode(new Symbol("inputs"));
        Object outputs = getObject(node, "outputs");
        Node body = node.getChildNode(null);

        Vector modifiers = new Vector();
        Vector inputs = new Vector();

        if(modifiersNode != null) {
            modifiers = modifiersNode.getChildren();
        }

        if(inputsNode != null) {
            inputs = inputsNode.getChildren();
        }


        if(name == null) {
            throw new RuntimeException("Method must have a name");
        }

        int access = 0;
        for(Object modifier : modifiers) {
            if(modifier.equals(new Symbol("public"))) {
                access = access + ACC_PUBLIC;
            } else if(modifier.equals(new Symbol("private"))) {
                access = access + ACC_PRIVATE;
            } else if(modifier.equals(new Symbol("protected"))) {
                access = access + ACC_PROTECTED;
            } else if(modifier.equals(new Symbol("static"))) {
                access = access + ACC_STATIC;
            } else if(modifier.equals(new Symbol("varargs"))) {
                access = access + ACC_VARARGS;
            }
        }

        Vector<Type> inputTypes = new Vector<Type>();
        Vector<Class> inputClasses = new Vector<Class>();
        Vector<Symbol> inputNames = new Vector<Symbol>();

        // TODO: handle resumable
        //inputTypes.add(Type.getType(ExecutionContext.class));
        //inputClasses.add(ExecutionContext.class);
        //inputNames.add(context.uniqueIdentifier("context:variable"));

        for(Object o : inputs) {
            if(o instanceof Symbol) {
                // This is the case where the user did not supply any type information

                // TODO: Change this to "Var"
                inputTypes.add(Type.getType(Object.class));
                inputClasses.add(Object.class);
                inputNames.add((Symbol)o);
            } else if(o instanceof Node) {
                Node n = (Node)o;

                Symbol variableName = (Symbol)n.getFirstChild();

                Object symbol = n.getSecondChild();
                Class klass = Compiler.resolveType(symbol, context);
                if(klass == null) {
                    throw new RuntimeException("Could not find symbol: " + symbol.toString());
                }

                inputTypes.add(Type.getType(klass));
                inputClasses.add(klass);
                inputNames.add(variableName);
            } else {
                throw new RuntimeException("Invalid input specification for function: " + o);
            }
        }

        Class outputClass = null;
        if(outputs == null) {
            // TODO: Make this Var?
            outputClass = Object.class;
        } else {
            outputClass = Compiler.resolveType(outputs, context);
            if(outputClass == null) {
                throw new RuntimeException("Could not resolve type: " + outputs);
            }
        }

        Method m = new Method(name.toString(), Type.getType(outputClass), inputTypes.toArray(new Type[0]));
        GeneratorAdapter g = new GeneratorAdapter(access, m, null, null, cw);

        String methodDescriptor = name.toString() + ":" + m.getDescriptor();
        if(PersistentMapHelper.contains(methods, methodDescriptor)) {
            throw new RuntimeException("Duplicate method");
        } else {
            methods = PersistentMapHelper.set(methods, methodDescriptor, Boolean.TRUE);
        }

        // Start a new frame...
        Class declaringClass = null;
        CompilationFrame frame = null;

        if(shouldEmit) {
            CompilationContext.SymbolEntry symbolEntry = context.symbolTable.get(fullyQualifiedName);
            if(symbolEntry != null) {
                // TODO: Handle resumable
                declaringClass = symbolEntry.klass;
                frame = new CompilationFrame(access, m, g, declaringClass, false, outputClass);
            } else {
                throw new RuntimeException("Internal error should have scaffolded: " + fullyQualifiedName);
            }
        } else {
            // TODO: Handle resumable
            frame = new CompilationFrame(access, m, g, null, false, outputClass);
        }

        context.frames.push(frame);

        // Handle local variables
        if(shouldEmit) {
            frame.newLocal(new Symbol("this"), declaringClass);

            if(inputClasses.size() == inputNames.size()) {
                for(int i = 0; i < inputClasses.size(); i++) {
                    frame.newLocal(inputNames.get(i), inputClasses.get(i));
                }
            } else {
                throw new RuntimeException("Internal error. The length of the input names and types should be the same.");
            }

            frame.newLocal(new Symbol("super"), declaringClass.getSuperclass());

            (new Block(body)).emit(context);
        } else {
            Node newBody = new Node(
                new Symbol("return"),
                Compiler.defaultValueForType(outputClass)
            );

            Compiler.buildExpression(newBody).emit(context);
        }

        (new Return(null, false)).emit(context);

        // End method
        context.frames.pop();
        g.endMethod();

        return methods;
    }

    public IPersistentMap doEmitField(Node node, CompilationContext context, IPersistentMap fields, ClassWriter cw) {
        /*field(
            name(i)
            type(int)
            modifiers(public)
            default(5)
        )*/

        Symbol name = getSymbol(node, "name");
        Object type = getObject(node, "type");
        Vector modifiers = node.getChildNode(new Symbol("modifiers")).getChildren();
        Object defaultValue = getObject(node, "default");

        if(name == null) {
            throw new RuntimeException("Field must have a name");
        } else {
            if(PersistentMapHelper.contains(fields, name)) {
                throw new RuntimeException("Duplicate field name");
            }
        }

        if(type == null) {
            type = new Symbol("Object");
        }

        Class klass = Compiler.resolveType(type, context);
        if(klass == null) {
            throw new RuntimeException("Could not resolve type: " + type);
        }

        int access = 0;
        for(Object modifier : modifiers) {
            if(modifier.equals(new Symbol("public"))) {
                access = access + ACC_PUBLIC;
            } else if(modifier.equals(new Symbol("private"))) {
                access = access + ACC_PRIVATE;
            } else if(modifier.equals(new Symbol("protected"))) {
                access = access + ACC_PROTECTED;
            } else if(modifier.equals(new Symbol("static"))) {
                access = access + ACC_STATIC;
            }
        }

        cw.visitField(access, name.toString(), Type.getType(klass).getDescriptor(), null, defaultValue).visitEnd();

        fields = PersistentMapHelper.set(fields, name, defaultValue);
        return fields;
    }

    public void doEmit(Node node, CompilationContext context, boolean shouldEmit) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        GeneratorAdapter g;
        AnnotationVisitor av;
        Method m;

        Symbol name = null;
        Class superClass = null;
        String superClassName = null;
        String[] interfaces = null;
        Node temp = null;



        // Get the name of the class
        temp = node.getChildNode(new Symbol("name"));
        if(temp != null) {
            if(temp.getFirstChild() instanceof Symbol) {
                name = (Symbol)temp.getFirstChild();
            } else {
                throw new RuntimeException("The name of a class must be a symbol.");
            }
        } else {
            throw new RuntimeException("The name of a class must be a symbol.");
        }



        // Get the name of the superclass
        temp = node.getChildNode(new Symbol("extends"));
        if(temp != null) {
            if(temp.getFirstChild() instanceof Symbol) {
                 Class klass = Compiler.resolveType(temp.getFirstChild(), context);
                 if(klass == null) {
                     throw new RuntimeException("Could not find symbol: " + temp.getFirstChild());
                 }

                 superClass = klass;
            } else {
                throw new RuntimeException("The name of a class's superclass must be a symbol.");
            }
        } else {
            superClass = Object.class;
        }

        superClassName = Type.getType(superClass).getInternalName();



        // Get the name of the interfaces
        temp = node.getChildNode(new Symbol("implements"));
        if(temp != null) {
            Vector children = temp.getChildren();
            interfaces = new String[children.size()];

            for(int i = 0; i < children.size(); i++) {
                Object iface = children.get(i);

                Class klass = Compiler.resolveType(iface, context);
                if(klass == null) {
                    throw new RuntimeException("Could not find symbol: " + iface);
                }

                interfaces[i] = Type.getType(klass).getInternalName();
            }
        }



        // Handle the meta data
        if(PersistentMapHelper.get(node.getMeta(), "file") == null) {
            cw.visitSource("UNKNOWN_FILE", null);
        } else {
            cw.visitSource(PersistentMapHelper.get(node.getMeta(), "file").toString(), null);
        }



        // The class definition
        String fullyQualifiedName = Compiler.fullyQualifiedName(name, context);
        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, fullyQualifiedName.replace(".", "/"), null, superClassName, interfaces);



        // Constructors
        m = Method.getMethod("void <init> ()");
        g = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cw);
        g.loadThis();
        g.invokeConstructor(Type.getType(superClass), m);
        g.returnValue();
        g.endMethod();



        // Handle fields
        IPersistentVector fieldNodes = node.getChildNodes(new Symbol("field"));
        IPersistentMap fields = PersistentMapHelper.create();

        for(int i = 0; i < PersistentVectorHelper.length(fieldNodes); i++) {
            Node field = (Node)PersistentVectorHelper.get(fieldNodes, i);
            fields = PersistentMapHelper.merge(fields, doEmitField(field, context, fields, cw));
        }



        // Handle methods
        IPersistentVector methodNodes = node.getChildNodes(new Symbol("method"));
        IPersistentMap methods = PersistentMapHelper.create();

        for(int i = 0; i < PersistentVectorHelper.length(methodNodes); i++) {
            Node method = (Node)PersistentVectorHelper.get(methodNodes, i);
            methods = PersistentMapHelper.merge(methods, doEmitMethod(method, context, methods, cw, fullyQualifiedName, shouldEmit));
        }



        // Wrap up
        cw.visitEnd();

        byte[] code = cw.toByteArray();



        /*
        if(shouldEmit) {
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
        }
        */



        if(shouldEmit) {
            Class klass = context.runtime.loader.loadClass(code);
            context.classes.add(klass);
            context.bytecode.add(code);

            CompilationContext.SymbolEntry entry = context.symbolTable.get(fullyQualifiedName);
            if(entry != null) {
                entry.compiled = true;
            }

            if(context.currentFrameExists()) {
                context.currentFrame().operandStack.push(Class.class);
                context.currentFrame().generator.visitLdcInsn(Type.getType(klass));
            }
        } else {
            CompilationContext.SymbolEntry entry = new CompilationContext.SymbolEntry();
            entry.name = fullyQualifiedName;
            entry.klass = context.symbolLoader.loadClass(code);
            entry.code = node;
            entry.namespace = context.currentNamespace();
            entry.compiled = false;

            context.symbolTable.put(entry.name, entry);
        }
    }
}
