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
    Symbol varargs;

    public static FunctionExpression build(Node node) {
        Symbol name = null;
        Vector inputs = null;
        Vector outputs = null;
        Block body = null;
        boolean macro = false;
        Symbol varargs = null;

        Node n = null;
        Object o = null;


        o = node.getChildNamed(new Symbol("macro"));
        macro = o != null;

        o = node.getChildNamed(new Symbol("varargs"));
        if(o != null) {
            if((o instanceof Node) && (((Node)o).getFirstChild() instanceof Symbol)) {
                o = ((Node)o).getFirstChild();
                varargs = (Symbol)o;
            } else {
                throw new RuntimeException("The name of a varargs parameter must be a symbol and cannot have a type.");
            }
        }

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

        // TODO: Should this be null instead? See grammar.g's comment about braces...
        n = node.getChildNode(new Symbol("do"));
        if(n != null) {
            body = Block.build(n);
        }

        return new FunctionExpression(name, inputs, outputs, body, macro, varargs);
    }

    public FunctionExpression(Symbol name, Vector inputs, Vector outputs, Block body, boolean macro, Symbol varargs) {
        this.name = name;
        this.inputs = inputs;
        this.outputs = outputs;
        this.body = body;
        this.macro = macro;
        this.varargs = varargs;
    }

    public Class type(CompilationContext context) {
        // TODO: Return function objects and method handles.
        // TODO: I think that I need to do forward declaration to get this to work... right?
        return Object.class;
    }

    public void emitDeclaration(CompilationContext context) {
        emit(context, false);
    }

    public void emit(CompilationContext context) {
        emit(context, true);
    }

    public void emit(CompilationContext context, boolean shouldEmit) {

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

                // TODO: Should I make this label/child instead of firstChild/secondChild. So: name(string)?
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
            // TODO: make this var when you implement vars and traits
            outputType = Type.getType(Object.class);
        }

        GeneratorAdapter g;
        AnnotationVisitor av;
        Method m;

        // The function class definition
        // TODO - Figure out how to propagate the file name
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visitSource("app.silo", null);

        String fullyQualifiedName = context.currentNamespace().packageName;
        if(fullyQualifiedName == null || fullyQualifiedName.equals("")) {
            fullyQualifiedName = name.toString();
        } else {
            fullyQualifiedName = fullyQualifiedName + "." + name.toString();
            fullyQualifiedName = fullyQualifiedName.replace(".", "/");
        }

        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, fullyQualifiedName, null, Type.getType(Function.class).getInternalName(), null);
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
            body.emit(context);
        } else {
            Node newBody = new Node(
                new Symbol("return"),
                Compiler.defaultValueForType(outputClass)
            );

            Compiler.buildExpression(newBody).emit(context);
            body.emitDeclaration(context);
        }

        (new Return(null, false)).emit(context);

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
        frame.generator.goTo(startLabel);

        context.frames.pop();
        // End the frame...

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

        if(shouldEmit) {
            Class klass = context.runtime.loader.loadClass(code);
            context.classes.add(klass);
            context.bytecode.add(code);
        } else {
            context.symbolLoader.loadClass(code);
        }
    }
}
