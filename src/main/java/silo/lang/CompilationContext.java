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

    public void clear() {
        this.frames.clear();
        this.namespaces.clear();

        this.classes.clear();
        this.bytecode.clear();

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
