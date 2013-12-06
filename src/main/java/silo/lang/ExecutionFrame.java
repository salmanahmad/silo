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

public class ExecutionFrame {
    public String id;
    public int programCounter;
    public Object[] locals;
    public Object[] stack;

    private Function function;

    // TODO - Make this less clean and allow for direct fields for storing locals and the stack.

    public ExecutionFrame() {
        
    }

    public ExecutionFrame(Function function) {
        this.id = function.getName();
        this.function = function;
    }

    public Function getFunction(RuntimeClassLoader loader) {
        if(this.function == null) {
            try {
                Class klass = Class.forName(this.id, true, loader);
                this.function = (Function)klass.newInstance();
            } catch(Exception e) {
                throw new RuntimeException("Could not get function for frame.");
            }
        }

        return this.function;
    }
}
