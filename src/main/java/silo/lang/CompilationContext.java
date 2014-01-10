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

    public final Runtime runtime;

    public final Stack<CompilationFrame> frames;
    public final Vector<Class> classes; // TODO: How do I handle temporary types and forwarded declarations?
    public final Vector<byte[]> bytecode;

    public final Vector<String> imports; // TODO: Make import a special form, probably. Alternatively, can I pass CompilationContext to macros?
    public final HashMap<String, String> aliases;
    public String packageName;

    int uniqueIdentifierCounter = 0;

    public CompilationContext(Runtime runtime) {
        this.runtime = runtime;

        this.frames = new Stack<CompilationFrame>();
        this.classes = new Vector<Class>();
        this.bytecode = new Vector<byte[]>();

        this.imports = new Vector<String>();
        this.aliases = new HashMap<String, String>();

        this.clear();
    }

    public CompilationFrame currentFrame() {
        return frames.peek();
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
        this.classes.clear();
        this.bytecode.clear();

        this.imports.clear();
        this.aliases.clear();

        this.packageName = "";
        this.imports.add("");
        this.imports.add("java.lang");
        this.imports.add("java.util");
        this.imports.add("java.io");
        this.imports.add("silo.core");
        this.aliases.put("IPersistentVector", "com.github.krukow.clj_lang.IPersistentVector");
    }
}
