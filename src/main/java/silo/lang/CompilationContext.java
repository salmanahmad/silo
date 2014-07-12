/*
 *
 *  Copyright 2012 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.lang;

import silo.lang.compiler.Compiler;

import java.util.UUID;
import java.util.Stack;
import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class CompilationContext {

    // TODO: These enum definitions could be useful to determine if compilation is performing
    // scaffolding, expansion, codegen, etc.
    /*
    public static enum Stage {}
    public static enum Phase {}
    public static enum Pass {}
    */

    public static class SymbolEntry {
        public String name;
        public Class klass;
        public Node code;
        public Namespace namespace;
        public boolean compiled;
    }

    // TODO: Is a better name for this "Environment"?
    public static class Namespace {
        // TODO: Make import a special form, probably. Alternatively, can I pass CompilationContext to macros?
        public String packageName;
        public final LinkedHashSet<String> imports = new LinkedHashSet<String>();
        public final HashMap<String, String> aliases = new HashMap<String, String>();

        public Namespace() {
            this.imports.add("");
        }

        public Namespace(Namespace ns) {
            this.imports.addAll(ns.imports);
            this.aliases.putAll(ns.aliases);
        }
    }

    public final Runtime runtime;

    public final Stack<CompilationFrame> frames;
    public final Stack<Namespace> namespaces;

    public final Vector<Class> classes; // TODO: How do I handle temporary types and forwarded declarations?
    public final Vector<byte[]> bytecode;

    public final HashMap<Stack<Class>, Class> customFrames;
    public final HashSet<String> files;

    public RuntimeClassLoader symbolLoader;
    public final LinkedHashMap<String, SymbolEntry> symbolTable;

    private int uniqueIdentifierCounter;
    private int finallyScaffoldingCount;

    public CompilationContext(Runtime runtime) {
        this.runtime = runtime;

        this.frames = new Stack<CompilationFrame>();
        this.namespaces = new Stack<Namespace>();

        this.classes = new Vector<Class>();
        this.bytecode = new Vector<byte[]>();

        this.customFrames = new HashMap<Stack<Class>, Class>();
        this.files = new HashSet<String>();

        this.symbolLoader = new RuntimeClassLoader();
        this.symbolTable = new LinkedHashMap<String, SymbolEntry>();

        this.uniqueIdentifierCounter = 0;
        this.finallyScaffoldingCount = 0;

        this.clear();
    }

    public boolean currentFrameExists() {
        return frames.size() != 0;
    }

    public CompilationFrame currentFrame() {
        return frames.peek();
    }

    public Namespace currentNamespace() {
        return namespaces.peek();
    }

    public synchronized Symbol uniqueIdentifier(String tag) {
        if(tag == null) {
            tag = "";
        }

        uniqueIdentifierCounter += 1;
        return new Symbol("__" + tag + "__" + uniqueIdentifierCounter);
    }

    public boolean isInsideFinallyClause() {
        return finallyScaffoldingCount != 0;
    }

    public synchronized void enterFinallyScaffold() {
        finallyScaffoldingCount++;
    }

    public synchronized void exitFinallyScaffold() {
        finallyScaffoldingCount--;
    }

    public Class customFrame(Stack<Class> ops) {
        Stack<Class> operands = (Stack<Class>)ops.clone();

        if(!customFrames.containsKey(operands)) {
            // Generate custom class

            String className = "frame_" + UUID.randomUUID().toString().replace("-", "_");
            String fullyQualifiedClassName = "silo.lang.rt.frames." + className;

            Vector children = new Vector();
            Vector inputs = new Vector();
            Vector code = new Vector();

            for(int i = 0; i < operands.size(); i++) {
                Class operand = operands.get(i);
                String name = "operand" + i;
                Object type = silo.lang.compiler.Parser.parse("java.lang.Object").getFirstChild();

                if(operand.isPrimitive()) {
                    type = new Symbol(operand.getName());
                }

                inputs.add(
                    new Node(
                        new Symbol(":"),
                        new Symbol(name),
                        type
                    )
                );

                code.add(
                    silo.lang.compiler.Parser.parse("frame." + name + " = " + name).getFirstChild()
                );

                Node field = new Node(
                    new Symbol("field"),
                    new Node(
                        new Symbol("name"),
                        new Symbol(name)
                    ),
                    new Node(
                        new Symbol("modifiers"),
                        new Symbol("public")
                    ),
                    new Node(
                        new Symbol("type"),
                        type
                    )
                );

                children.add(field);
            }

            children.add(new Node(
                new Symbol("name"),
                new Symbol(className)
            ));

            children.add(new Node(
               new Symbol("extends"),
               silo.lang.compiler.Parser.parse("silo.lang.ExecutionFrame").getFirstChild()
            ));

            children.add(new Node(
                new Symbol("method"),
                new Node(
                    new Symbol("name"),
                    new Symbol("build")
                ),
                new Node(
                    new Symbol("modifiers"),
                    new Symbol("public"),
                    new Symbol("static")
                ),
                new Node(
                    new Symbol("outputs"),
                    silo.lang.compiler.Parser.parse(fullyQualifiedClassName).getFirstChild()
                ),
                new Node(
                    new Symbol("inputs"),
                    inputs
                ),
                new Node(
                    null,
                    silo.lang.compiler.Parser.parse("frame : " + fullyQualifiedClassName + " = " + fullyQualifiedClassName + "()").getFirstChild(),
                    new Node(
                        null,
                        code
                    ),
                    silo.lang.compiler.Parser.parse("return(frame)").getFirstChild()
                )
            ));

            Node klass = new Node(
                new Symbol("defineclass"),
                children
            );

            Stack<CompilationFrame> compilationFrames = this.saveFrames();

            this.namespaces.push(new Namespace(this.currentNamespace()));
            this.currentNamespace().packageName = "silo.lang.rt.frames";

            Object scaffolded = Compiler.buildExpression(klass).scaffold(this);
            Compiler.buildExpression(scaffolded).emit(this);

            this.restoreFrames(compilationFrames);

            customFrames.put(operands, this.classes.lastElement());

            this.namespaces.pop();
        }

        return customFrames.get(operands);
    }

    public Stack<CompilationFrame> saveFrames() {
        Stack<CompilationFrame> frames = new Stack<CompilationFrame>();
        for(CompilationFrame frame : this.frames) {
            frames.push(frame);
        }

        this.frames.clear();
        return frames;
    }

    public void restoreFrames(Stack<CompilationFrame> frames) {
        for(CompilationFrame frame : frames) {
            this.frames.push(frame);
        }
    }

    public void clear() {
        this.frames.clear();
        this.namespaces.clear();

        this.classes.clear();
        this.bytecode.clear();

        this.customFrames.clear();
        this.files.clear();

        this.symbolLoader = new RuntimeClassLoader();
        this.symbolTable.clear();

        this.namespaces.clear();
        this.namespaces.push(new Namespace());
        this.currentNamespace().packageName = "";
        this.currentNamespace().imports.add("");
        this.currentNamespace().imports.add("java.lang");
        this.currentNamespace().imports.add("java.util");
        this.currentNamespace().imports.add("java.io");
        this.currentNamespace().imports.add("silo.core");
        this.currentNamespace().aliases.put("Function", "silo.lang.Function");

        this.currentNamespace().aliases.put("fs", "java.nio.file.Files");
        this.currentNamespace().aliases.put("Path", "java.nio.file.Path");

        this.currentNamespace().aliases.put("Vector", "com.github.krukow.clj_lang.IPersistentVector");
        this.currentNamespace().aliases.put("Map", "com.github.krukow.clj_lang.IPersistentMap");

        this.currentNamespace().aliases.put("vector", "silo.lang.PersistentVectorHelper");
        this.currentNamespace().aliases.put("map", "silo.lang.PersistentMapHelper");
    }
}
