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

public class CompilationContext {

    public static class SymbolEntry {
        public String name;
        public Class klass;
        public Node code;
        public Namespace namespace;
    }

    public static class Namespace {
        // TODO: Make import a special form, probably. Alternatively, can I pass CompilationContext to macros?
        public String packageName;
        public final Vector<String> imports = new Vector<String>();
        public final HashMap<String, String> aliases = new HashMap<String, String>();
    }

    public final Runtime runtime;

    public final Stack<CompilationFrame> frames;
    public final Stack<Namespace> namespaces;

    public final Vector<Class> classes; // TODO: How do I handle temporary types and forwarded declarations?
    public final Vector<byte[]> bytecode;

    public RuntimeClassLoader symbolLoader;
    public final HashMap<String, SymbolEntry> symbolTable;

    private int uniqueIdentifierCounter;

    public CompilationContext(Runtime runtime) {
        this.runtime = runtime;

        this.frames = new Stack<CompilationFrame>();
        this.namespaces = new Stack<Namespace>();

        this.classes = new Vector<Class>();
        this.bytecode = new Vector<byte[]>();

        this.symbolLoader = new RuntimeClassLoader();
        this.symbolTable = new HashMap<String, SymbolEntry>();

        this.uniqueIdentifierCounter = 0;

        this.clear();
    }

    public CompilationFrame currentFrame() {
        return frames.peek();
    }

    public Namespace currentNamespace() {
        return namespaces.peek();
    }

    public Symbol uniqueIdentifier(String tag) {
        if(tag == null) {
            tag = "";
        }

        uniqueIdentifierCounter += 1;
        return new Symbol("__" + tag + "__" + uniqueIdentifierCounter);
    }

    public void clear() {
        this.frames.clear();
        this.namespaces.clear();

        this.classes.clear();
        this.bytecode.clear();

        this.symbolLoader = new RuntimeClassLoader();
        this.symbolTable.clear();

        this.namespaces.push(new Namespace());
        this.currentNamespace().packageName = "";
        this.currentNamespace().imports.add("");
        this.currentNamespace().imports.add("java.lang");
        this.currentNamespace().imports.add("java.util");
        this.currentNamespace().imports.add("java.io");
        this.currentNamespace().imports.add("silo.core");
        this.currentNamespace().aliases.put("IPersistentVector", "com.github.krukow.clj_lang.IPersistentVector");
        this.currentNamespace().aliases.put("RT", "com.github.krukow.clj_lang.RT");
    }
}
