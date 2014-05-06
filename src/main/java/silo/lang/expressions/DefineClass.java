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

import org.apache.commons.lang3.StringUtils;

// Note: Constructors and static initializers are *never* resumable.

public class DefineClass implements Expression, Opcodes {

    Node node;

    public DefineClass(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Class.class;
    }

    private static boolean classesAreEqual(Object symbol, Symbol className, CompilationContext context) {
        if(symbol instanceof Symbol) {
            return symbol.equals(className);
        } else if(symbol instanceof Node) {
            String fullyQualifiedName = Compiler.fullyQualifiedName(className, context);
            Vector<Symbol> list = Compiler.symbolList(symbol);

            if(list != null) {
                return fullyQualifiedName.equals(StringUtils.join(list, "."));
            }
        }

        return false;
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

    public IPersistentMap doEmitConstructor(Node node, CompilationContext context, IPersistentMap ctors, IPersistentMap fields, ClassWriter cw, Symbol className, boolean shouldEmit) {
        /*constructor(
            resumable(true) // Not allowed
            modifiers(public, varargs)
            inputs(int) {
                ...
            }
        )*/

        String fullyQualifiedName = Compiler.fullyQualifiedName(className, context);

        Node modifiersNode = node.getChildNode(new Symbol("modifiers"));
        Node inputsNode = node.getChildNode(new Symbol("inputs"));
        Node body = node.getChildNode(null);
        Node resumableNode = node.getChildNode(new Symbol("resumable"));

        Vector modifiers = new Vector();
        Vector inputs = new Vector();

        if(modifiersNode != null) {
            modifiers = modifiersNode.getChildren();
        }

        if(resumableNode != null) {
            throw new RuntimeException("Silo cannot make constructors resumable at this time. Sorry. Is refactoring your code at all possible?");
        }

        if(inputsNode != null) {
            inputs = inputsNode.getChildren();
        }

        int access = 0;
        for(Object modifier : modifiers) {
            if(modifier.equals(new Symbol("public"))) {
                access = access + ACC_PUBLIC;
            } else if(modifier.equals(new Symbol("private"))) {
                access = access + ACC_PRIVATE;
            } else if(modifier.equals(new Symbol("protected"))) {
                access = access + ACC_PROTECTED;
            } else if(modifier.equals(new Symbol("varargs"))) {
                access = access + ACC_VARARGS;
            }
        }

        Vector<Type> inputTypes = new Vector<Type>();
        Vector<Class> inputClasses = new Vector<Class>();
        Vector<Symbol> inputNames = new Vector<Symbol>();

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
                    if(classesAreEqual(symbol, className, context)) {
                        inputTypes.add(Type.getObjectType(fullyQualifiedName.replace(".", "/")));
                        inputClasses.add(Object.class); // Just make it object for now...
                        inputNames.add(variableName);
                    } else {
                        throw new RuntimeException("Could not find symbol: " + symbol.toString());
                    }
                } else {
                    inputTypes.add(Type.getType(klass));
                    inputClasses.add(klass);
                    inputNames.add(variableName);
                }
            } else {
                throw new RuntimeException("Invalid input specification for function: " + o);
            }
        }

        Method m = new Method("<init>", Type.getType(Void.TYPE), inputTypes.toArray(new Type[0]));
        GeneratorAdapter g = new GeneratorAdapter(access, m, null, null, cw);

        String methodDescriptor = m.getDescriptor();
        if(PersistentMapHelper.contains(ctors, methodDescriptor)) {
            throw new RuntimeException("Duplicate constructor");
        } else {
            ctors = PersistentMapHelper.set(ctors, methodDescriptor, Boolean.TRUE);
        }

        // Start a new frame...
        Class declaringClass = null;
        CompilationFrame frame = null;

        if(shouldEmit) {
            CompilationContext.SymbolEntry symbolEntry = context.symbolTable.get(fullyQualifiedName);
            if(symbolEntry != null) {
                declaringClass = symbolEntry.klass;
                frame = new CompilationFrame(access, m, g, declaringClass, false, Void.TYPE);
            } else {
                throw new RuntimeException("Internal error should have scaffolded: " + fullyQualifiedName);
            }
        } else {
            frame = new CompilationFrame(access, m, g, null, false, Void.TYPE);
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

            // Invoke super constructor
            g.loadThis();
            g.invokeConstructor(Type.getType(declaringClass.getSuperclass()), Method.getMethod("void <init> ()"));

            // Set the fields
            IPersistentVector fieldsVector = PersistentMapHelper.keys(fields);
            for(int i = 0; i < PersistentVectorHelper.length(fieldsVector); i++) {
                Object fieldName = PersistentVectorHelper.get(fieldsVector, i);
                Object fieldValue = PersistentMapHelper.get(fields, fieldName);

                if(fieldValue == null) {
                    continue;
                }

                Node fieldAssignment = new Node(new Symbol("="),
                    new Node(new Symbol("."),
                        new Symbol("this"),
                        fieldName
                    ),
                    fieldValue
                );

                Compiler.buildExpression(fieldAssignment).emit(context);
            }

            frame.newLocal(new Symbol("constructor:variable"), Object[].class);
            (new Block(body)).emit(context);
            g.returnValue();
        } else {
            // Invoke super constructor just to avoid verification errors
            g.loadThis();
            g.invokeConstructor(Type.getType(Object.class), Method.getMethod("void <init> ()"));

            g.returnValue();
        }

        // End method
        context.frames.pop();
        g.endMethod();

        return ctors;
    }

    public IPersistentMap doEmitMethod(Node node, CompilationContext context, IPersistentMap methods, ClassWriter cw, Symbol className, boolean shouldEmit) {
        /*method(
            resumable(true) // Optional
            name(i)
            modifiers(static, public)
            inputs(int)
            outputs(int) {
                ...
            }
        )*/

        String fullyQualifiedName = Compiler.fullyQualifiedName(className, context);

        Symbol name = getSymbol(node, "name");
        Node modifiersNode = node.getChildNode(new Symbol("modifiers"));
        Node inputsNode = node.getChildNode(new Symbol("inputs"));
        Object outputs = getObject(node, "outputs");
        Node body = node.getChildNode(null);
        Node resumableNode = node.getChildNode(new Symbol("resumable"));

        // By default java methods are NOT resumable
        boolean resumable = false;

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

        if(resumableNode != null) {
            if(resumableNode.getFirstChild() != null && resumableNode.getFirstChild().equals(Boolean.TRUE)) {
                resumable = true;
            }
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

        if(resumable) {
            inputTypes.add(Type.getType(ExecutionContext.class));
            inputClasses.add(ExecutionContext.class);
            inputNames.add(context.uniqueIdentifier("context:variable"));
        }

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
                    if(classesAreEqual(symbol, className, context)) {
                        inputTypes.add(Type.getObjectType(fullyQualifiedName.replace(".", "/")));
                        inputClasses.add(Object.class); // Just make it object for now...
                        inputNames.add(variableName);
                    } else {
                        throw new RuntimeException("Could not find symbol: " + symbol.toString());
                    }
                } else {
                    inputTypes.add(Type.getType(klass));
                    inputClasses.add(klass);
                    inputNames.add(variableName);
                }
            } else {
                throw new RuntimeException("Invalid input specification for function: " + o);
            }
        }

        Type outputType = null;
        Class outputClass = null;
        if(outputs == null) {
            // TODO: Make this Var?
            outputClass = Object.class;
            outputType = Type.getType(Object.class);
        } else {
            outputClass = Compiler.resolveType(outputs, context);
            if(outputClass == null) {
                if(classesAreEqual(outputs, className, context)) {
                    outputClass = Object.class;
                    outputType = Type.getObjectType(fullyQualifiedName.replace(".", "/"));
                } else {
                    throw new RuntimeException("Could not find symbol: " + outputs.toString());
                }
            } else {
                outputType = Type.getType(outputClass);
            }
        }

        Method m = new Method(name.toString(), outputType, inputTypes.toArray(new Type[0]));
        GeneratorAdapter g = new GeneratorAdapter(access, m, null, null, cw);

        if(resumable) {
            AnnotationVisitor av = g.visitAnnotation(Type.getType(Resumable.class).getDescriptor(), true);
            av.visitEnd();
        }

        String methodDescriptor = name.toString() + ":" + m.getDescriptor();
        if(PersistentMapHelper.contains(methods, methodDescriptor)) {
            throw new RuntimeException("Duplicate method");
        } else {
            // Check if there is a duplicated method when considering resumability

            Vector<Type> tempTypes = (Vector<Type>)inputTypes.clone();
            if(resumable) {
                tempTypes.remove(0);
            } else {
                tempTypes.add(0, Type.getType(ExecutionContext.class));
            }

            String tempDesc = name.toString() + ":" + (new Method(name.toString(), outputType, tempTypes.toArray(new Type[0]))).getDescriptor();

            if(PersistentMapHelper.contains(methods, tempDesc)) {
                throw new RuntimeException("Attempting to overload a resumable method. Unfortunately, right now, you cannot have a method with the same name and same arguments except one is resumable and the other is not. That is not supported. Can you possible change the method name to something else?");
            }

            methods = PersistentMapHelper.set(methods, methodDescriptor, Boolean.TRUE);
        }

        // Start a new frame...
        Class declaringClass = null;
        CompilationFrame frame = null;

        if(shouldEmit) {
            CompilationContext.SymbolEntry symbolEntry = context.symbolTable.get(fullyQualifiedName);
            if(symbolEntry != null) {
                declaringClass = symbolEntry.klass;
                frame = new CompilationFrame(access, m, g, declaringClass, resumable, outputClass);
            } else {
                throw new RuntimeException("Internal error should have scaffolded: " + fullyQualifiedName);
            }
        } else {
            frame = new CompilationFrame(access, m, g, null, resumable, outputClass);
        }

        context.frames.push(frame);



        if(shouldEmit) {
            // Prelude to method body
            frame.restoreLocalsLabel = frame.generator.newLabel();
            frame.captureLocalsLabel = frame.generator.newLabel();

            // TODO: Optimize this so I dont' have to be jumping all over the place...
            // TODO: Intead of doing this...leverage the Expression abstraction and walk the syntax tree and extract all of the local variables along with their types.
            Label initializationLabel = frame.generator.newLabel();
            Label startLabel = frame.generator.newLabel();

            if(resumable) {
                // If I am resumable then go to my initialize variables phase.
                // TODO: Optimize this call out with my new lazy coroutine code...
                frame.generator.goTo(initializationLabel);
            }

            frame.generator.mark(startLabel);

            if ((access & ACC_STATIC) == 0) {
                // This is a virtual method
                inputNames.add(0, new Symbol("this"));
                inputTypes.add(0, Type.getType(declaringClass));
                inputClasses.add(0, declaringClass);
            }

            if(inputClasses.size() == inputNames.size()) {
                for(int i = 0; i < inputClasses.size(); i++) {
                    frame.newLocal(inputNames.get(i), inputClasses.get(i));
                }
            } else {
                throw new RuntimeException("Internal error. The length of the input names and types should be the same.");
            }

            frame.newLocal(new Symbol("constructor:variable"), Object[].class);
            (new Block(body)).emit(context);
            (new Return(null, false)).emit(context);


            // Local Initialization
            if(resumable) {
                frame.generator.mark(initializationLabel);
                for(Symbol local : frame.locals.keySet()) {
                    if(inputNames.contains(local)) {
                        continue;
                    }

                    int index = frame.locals.get(local);
                    Class klass = frame.localTypes.get(local);

                    // TODO: Redo this by using the AssignExpression. Just create a node and pass it into `Assign.build` or `new Assign`
                    Compiler.pushInitializationValue(klass, frame.generator);
                    frame.generator.visitVarInsn(Type.getType(klass).getOpcode(Opcodes.ISTORE), index);
                }
                Label[] resumeLabels = frame.resumeLabels();
                if(resumeLabels.length == 0) {
                    frame.generator.goTo(startLabel);
                } else {
                    Compiler.loadExecutionContext(context);
                    frame.generator.getField(Type.getType(ExecutionContext.class), "programCounter", Type.getType(int.class));
                    frame.generator.visitTableSwitchInsn(0, resumeLabels.length - 1, startLabel, resumeLabels);
                }

                // Local Restoration
                Label invalidProgamCounterLabel = frame.generator.mark();
                frame.generator.throwException(Type.getType(RuntimeException.class), "Invalid program counter");
                frame.generator.mark(frame.restoreLocalsLabel);
                for(Symbol variableName : frame.locals.keySet()) {
                    int variableIndex = frame.locals.get(variableName).intValue();
                    Class variableType = frame.localTypes.get(variableName);

                    if(variableIndex == 0) {
                        continue;
                    }

                    // TODO: Is this more or less efficient than doing weird DUP / DUPX2 / Swaps
                    Compiler.loadExecutionFrame(context);
                    frame.generator.getField(Type.getType(ExecutionFrame.class), "locals", Type.getType(Object[].class));
                    frame.generator.push(variableIndex);
                    frame.generator.arrayLoad(Type.getType(Object.class));

                    frame.generator.unbox(Type.getType(variableType));
                    frame.generator.visitVarInsn(Type.getType(variableType).getOpcode(Opcodes.ISTORE), variableIndex);
                }
                Compiler.loadExecutionContext(context);
                frame.generator.getField(Type.getType(ExecutionContext.class), "programCounter", Type.getType(int.class));
                Label[] continuationLabels = frame.continuationLabels(invalidProgamCounterLabel);
                frame.generator.visitTableSwitchInsn(0, continuationLabels.length - 1, continuationLabels[continuationLabels.length - 1], continuationLabels);

                // Local Capture
                frame.generator.mark(frame.captureLocalsLabel);
                Compiler.loadExecutionFrame(context);
                frame.generator.push(frame.nextLocal());
                frame.generator.newArray(Type.getType(Object.class));
                frame.generator.putField(Type.getType(ExecutionFrame.class), "locals", Type.getType(Object[].class));
                for(Symbol variableName : frame.locals.keySet()) {
                    int variableIndex = frame.locals.get(variableName).intValue();
                    Class variableType = frame.localTypes.get(variableName);

                    if(variableIndex == 0) {
                        continue;
                    }

                    // TODO: Is this more or less efficient than doing weird DUP / DUPX2 / Swaps
                    Compiler.loadExecutionFrame(context);
                    frame.generator.getField(Type.getType(ExecutionFrame.class), "locals", Type.getType(Object[].class));
                    frame.generator.push(variableIndex);
                    frame.generator.visitVarInsn(Type.getType(variableType).getOpcode(Opcodes.ILOAD), variableIndex);
                    frame.generator.box(Type.getType(variableType));
                    frame.generator.arrayStore(Type.getType(Object.class));
                }
                Compiler.pushInitializationValue(frame.outputClass, frame.generator);
                frame.generator.returnValue();
            }
        } else {
            Node newBody = new Node(
                new Symbol("return"),
                Compiler.defaultValueForType(outputClass)
            );

            Compiler.buildExpression(newBody).emit(context);
            (new Return(null, false)).emit(context);
        }

        // End method
        context.frames.pop();
        g.endMethod();

        return methods;
    }

    public IPersistentMap doEmitField(Node node, CompilationContext context, IPersistentMap fields, ClassWriter cw, Symbol className) {
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

        Type outputType = null;
        Class klass = Compiler.resolveType(type, context);
        if(klass == null) {
            if(classesAreEqual(type, className, context)) {
                outputType = Type.getObjectType(Compiler.fullyQualifiedName(className, context).replace(".", "/"));
            } else {
                throw new RuntimeException("Could not resolve type: " + type);
            }
        } else {
            outputType = Type.getType(klass);
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

        cw.visitField(access, name.toString(), outputType.getDescriptor(), null, defaultValue).visitEnd();

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
            if((temp.getFirstChild() instanceof Symbol) || (temp.getFirstChild() instanceof Node)) {
                 Class klass = Compiler.resolveType(temp.getFirstChild(), context);
                 if(klass == null) {
                     throw new RuntimeException("Could not find symbol: " + temp.getFirstChild());
                 }

                 superClass = klass;
            } else {
                throw new RuntimeException("The name of a class's superclass must be a symbol or a node.");
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



        // Handle fields
        IPersistentVector fieldNodes = node.getChildNodes(new Symbol("field"));
        IPersistentMap fields = PersistentMapHelper.create();

        for(int i = 0; i < PersistentVectorHelper.length(fieldNodes); i++) {
            Node field = (Node)PersistentVectorHelper.get(fieldNodes, i);
            fields = PersistentMapHelper.merge(fields, doEmitField(field, context, fields, cw, name));
        }



        // Handle constructors
        IPersistentVector constructorNodes = node.getChildNodes(new Symbol("constructor"));
        IPersistentMap constructors = PersistentMapHelper.create();

        if(PersistentVectorHelper.length(constructorNodes) == 0) {
            // Add the default constructor
            m = Method.getMethod("void <init> ()");
            g = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cw);
            g.loadThis();
            g.invokeConstructor(Type.getType(superClass), m);
            g.returnValue();
            g.endMethod();
        } else {
            for(int i = 0; i < PersistentVectorHelper.length(constructorNodes); i++) {
                Node constructor = (Node)PersistentVectorHelper.get(constructorNodes, i);
                constructors = PersistentMapHelper.merge(constructors, doEmitConstructor(constructor, context, constructors, fields, cw, name, shouldEmit));
            }
        }



        // Handle methods
        IPersistentVector methodNodes = node.getChildNodes(new Symbol("method"));
        IPersistentMap methods = PersistentMapHelper.create();

        for(int i = 0; i < PersistentVectorHelper.length(methodNodes); i++) {
            Node method = (Node)PersistentVectorHelper.get(methodNodes, i);
            methods = PersistentMapHelper.merge(methods, doEmitMethod(method, context, methods, cw, name, shouldEmit));
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
