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

import java.util.Vector;
import java.util.Stack;
import java.util.HashMap;

import org.objectweb.asm.Label;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.commons.GeneratorAdapter;

public class CompilationFrame {

    public static class CallSite {
        public Label resumeSite;
        public Label continuationSite;
    }

    private int nextLocal = 0;

    public final int access;
    public final Method method;

    // If this is null, then it means that we are compiling a Function and not a normal Java class.
    public final Class declaringClass;
    public final boolean resumable;

    public GeneratorAdapter generator;

    public final Class outputClass;
    public final Stack<Class> operandStack;

    public final HashMap<Symbol, Integer> locals;
    public final HashMap<Symbol, Class> localTypes;

    public final Stack<Label> iterationFrameStartLabels;
    public final Stack<Label> iterationFrameEndLabels;
    public final Stack finallyClauses;

    public final Stack<CallSite> callSites;
    public Label restoreLocalsLabel;
    public Label captureLocalsLabel;

    public CompilationFrame(int access, Method method, GeneratorAdapter generator, Class declaringClass, boolean resumable, Class outputClass) {
        this.access = access;
        this.method = method;

        this.declaringClass = declaringClass;
        this.resumable = resumable;

        this.generator = generator;
        this.outputClass = outputClass;

        this.operandStack = new Stack<Class>();
        this.locals = new HashMap<Symbol, Integer>();
        this.localTypes = new HashMap<Symbol, Class>();

        this.iterationFrameStartLabels = new Stack<Label>();
        this.iterationFrameEndLabels = new Stack<Label>();
        this.finallyClauses = new Stack();

        this.callSites = new Stack<CallSite>();
    }

    public int newLocal(Symbol name, Class type) {
        int local = nextLocal;

        if(locals.containsKey(name)) {
            throw new RuntimeException("Duplicated variable name.");
        }

        locals.put(name, local);
        localTypes.put(name, type);

        if(type.equals(Double.TYPE) || type.equals(Long.TYPE)) {
            nextLocal += 2;
        } else {
            nextLocal += 1;
        }

        return local;
    }

    public int nextLocal() {
        return nextLocal;
    }

    public void pushIterationFrame(Label start, Label end) {
        iterationFrameStartLabels.push(start);
        iterationFrameEndLabels.push(end);
    }

    public void popIterationFrame() {
        iterationFrameStartLabels.pop();
        iterationFrameEndLabels.pop();
    }

    public Label[] resumeLabels() {
        Vector<Label> vec = new Vector<Label>();
        for(CallSite site : callSites) {
            vec.add(site.resumeSite);
        }

        return vec.toArray(new Label[0]);
    }

    public Label[] continuationLabels(Label invalidProgamCounterLabel) {
        Vector<Label> vec = new Vector<Label>();
        for(CallSite site : callSites) {
            vec.add(site.continuationSite);
        }

        vec.add(invalidProgamCounterLabel);

        return vec.toArray(new Label[0]);
    }
}
