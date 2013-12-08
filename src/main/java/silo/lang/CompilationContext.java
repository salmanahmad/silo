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

public class CompilationContext {

    public final Runtime runtime;
    public final Stack<CompilationFrame> frames;
    public final Vector<Class> classes;

    int uniqueIdentifierCounter = 0;

    public CompilationContext(Runtime runtime) {
        this.runtime = runtime;
        this.frames = new Stack<CompilationFrame>();
        this.classes = new Vector<Class>();
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
    }
}
