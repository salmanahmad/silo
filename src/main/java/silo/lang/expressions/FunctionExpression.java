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


// TODO: Should I rename all of the other expressions so that they have the "Expression" suffix?
// TODO: Should I call this a LiteralFunction? Or should I call LiteralArray ArrayExpression and ArrayTypeExpression?

public class FunctionExpression implements Expression, Opcodes {

    Node node;

    public FunctionExpression(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        // TODO: Should I return the actual class so that I can optimize
        // invoke performance of functors and provide better type safety?
        return Function.class;
    }

    public Object scaffold(CompilationContext context) {
        Node name = null;
        if(node.getChildNamed(new Symbol("name")) == null) {
            name = new Node(new Symbol("name"), context.uniqueIdentifier("function"));
        }

        Vector children = new Vector();
        for(Object child : node.getChildren()) {
            if(child instanceof Node) {
                // TODO: Should this be null instead? Perhaps just node.getLastChild()? See grammar.g's comment about braces... Also see the comment in emit()
                if(((Node)child).getLabel() == null) {
                    continue;
                }

                if(((Node)child).getLabel().equals(new Symbol("do"))) {
                    continue;
                }
            }

            children.add(child);
        }

        if(name != null) {
            children.add(name);
        }

        Object body = node.getChildNode(new Symbol("do"));
        if(body == null) {
            body = node.getChildNode(null);
        }
        if(body != null) {
            body = Compiler.buildExpression(body).scaffold(context);
            children.add(body);
        }

        Node scaffolded = new Node(node.getLabel(), children);
        scaffolded.meta = node.meta;

        doEmit(scaffolded, context, false);

        return scaffolded;
    }

    public String fullyQualifiedName(Symbol name, CompilationContext context) {
        String fullyQualifiedName = context.currentNamespace().packageName;
        if(fullyQualifiedName == null || fullyQualifiedName.equals("")) {
            fullyQualifiedName = name.toString();
        } else {
            fullyQualifiedName = fullyQualifiedName + "." + name.toString();
        }

        return fullyQualifiedName;
    }

    public void emit(CompilationContext context) {
        doEmit(node, context, true);
    }

    public void doEmit(Node node, CompilationContext context, boolean shouldEmit) {
        Symbol name = null;
        Vector inputs = null;
        Vector outputs = null;
        Block body = null;
        boolean macro = false;
        Symbol varargs = null;

        Node tempNode = null;
        Object tempObject = null;


        tempObject = node.getChildNamed(new Symbol("macro"));
        macro = tempObject != null;

        tempObject = node.getChildNamed(new Symbol("varargs"));
        if(tempObject != null) {
            if((tempObject instanceof Node) && (((Node)tempObject).getFirstChild() instanceof Symbol)) {
                tempObject = ((Node)tempObject).getFirstChild();
                varargs = (Symbol)tempObject;
            } else {
                throw new RuntimeException("The name of a varargs parameter must be a symbol and cannot have a type.");
            }
        }

        tempNode = node.getChildNode(new Symbol("name"));
        if(tempNode != null) {
            tempObject = tempNode.getFirstChild();
            if(tempObject instanceof Symbol) {
                name = (Symbol)tempObject;
            } else {
                throw new RuntimeException("The name of a function must be a symbol.");
            }
        }

        tempNode = node.getChildNode(new Symbol("inputs"));
        if(tempNode != null) {
            inputs = tempNode.getChildren();
        }

        tempNode = node.getChildNode(new Symbol("outputs"));
        if(tempNode != null) {
            outputs = tempNode.getChildren();
        }

        // TODO: Should this be null instead? Perhaps just node.getLastChild()? See grammar.g's comment about braces... Also see the comment in scaffold()
        tempNode = node.getChildNode(new Symbol("do"));
        if(tempNode == null) {
            tempNode = node.getChildNode(null);
        }
        if(tempNode != null) {
            body = new Block(tempNode);
        }






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







        String fullyQualifiedName = fullyQualifiedName(name, context);
        CompilationContext.SymbolEntry symbolEntry = context.symbolTable.get(fullyQualifiedName);
        if(symbolEntry != null) {
            if(symbolEntry.compiled) {
                return;
            } else {
                if(!shouldEmit) {
                    // We are inside of a scaffolding pass...
                    if(context.isInsideFinallyClause()) {
                        return;
                    } else {
                        throw new RuntimeException("Attempting to re-define a function named: " + fullyQualifiedName);
                    }
                }
            }
        } else {
            if(shouldEmit) {
                throw new RuntimeException("Internal error, did not scaffold: " + fullyQualifiedName);
            }
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

        inputTypes.add(Type.getType(ExecutionContext.class));
        inputClasses.add(ExecutionContext.class);
        inputNames.add(context.uniqueIdentifier("context:variable"));

        for(Object o : inputs) {
            // TODO: Add these inputs to the local variables

            if(o instanceof Symbol) {
                // This is the case where the user did not supply any type information

                // TODO: Change this to "Var"
                inputTypes.add(Type.getType(Object.class));
                inputClasses.add(Object.class);
                inputNames.add((Symbol)o);
            } else if(o instanceof Node) {
                Node n = (Node)o;

                // TODO: Should I make this label/child instead of firstChild/secondChild. So: name(string)?
                // TODO: What about generics or arrays?
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

        if(varargs != null) {
            inputTypes.add(Type.getType(IPersistentVector.class));
            inputClasses.add(IPersistentVector.class);
            inputNames.add(varargs);
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
            // TODO: make this Var when you implement vars and traits
            outputType = Type.getType(Object.class);
            // This line used to be commented out and allowed to stay Void.TYPE
            // so that I could distinguish between Object / Var returns and
            // Void returns inside the Return expression.
            outputClass = Object.class;
        }

        GeneratorAdapter g;
        AnnotationVisitor av;
        Method m;

        // The function class definition
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        if(node.getMeta().get("file") == null) {
            cw.visitSource("UNKNOWN_FILE", null);
        } else {
            cw.visitSource(node.getMeta().get("file").toString(), null);
        }

        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, fullyQualifiedName.replace(".", "/"), null, Type.getType(Function.class).getInternalName(), null);
        av = cw.visitAnnotation(Type.getType(Function.Definition.class).getDescriptor(), true);
        if(macro) {
            av.visit("macro", Boolean.TRUE);
        }
        if(varargs != null) {
            // TODO: Varargs should also create an actual overloaded varargs method that wraps a PersistentVector and calls the actual method..
            av.visit("varargs", Boolean.TRUE);
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

        // Start a new frame...
        CompilationFrame frame = new CompilationFrame(ACC_PUBLIC + ACC_STATIC, m, g, outputClass);
        context.frames.push(frame);

        frame.restoreLocalsLabel = frame.generator.newLabel();
        frame.captureLocalsLabel = frame.generator.newLabel();

        if(inputClasses.size() == inputNames.size()) {
            for(int i = 0; i < inputClasses.size(); i++) {
                frame.newLocal(inputNames.get(i), inputClasses.get(i));
            }
        } else {
            throw new RuntimeException("Internal error. The length of the input names and types should be the same.");
        }

        // TODO: Optimize this so I dont' have to be jumping all over the place...
        // TODO: Intead of doing this...leverage the Expression abstraction and walk the syntax tree and
        // extract all of the local variables along with their types.
        Label initializationLabel = frame.generator.newLabel();
        Label startLabel = frame.generator.newLabel();
        frame.generator.goTo(initializationLabel);

        frame.generator.mark(startLabel);

        if(shouldEmit) {
            frame.newLocal(new Symbol("constructor:variable"), Object[].class);
            body.emit(context);
        } else {
            Node newBody = new Node(
                new Symbol("return"),
                Compiler.defaultValueForType(outputClass)
            );

            Compiler.buildExpression(newBody).emit(context);
        }

        (new Return(null, false)).emit(context);

        // Local Initialization
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

        context.frames.pop();
        // End the frame...

        g.endMethod();

        // TODO: Virtual apply methods from Function.class

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
                context.currentFrame().operandStack.push(Function.class);

                context.currentFrame().generator.newInstance(Type.getType(klass));
                context.currentFrame().generator.dup();
                context.currentFrame().generator.invokeConstructor(
                    Type.getType(klass),
                    Method.getMethod("void <init> ()")
                );
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
