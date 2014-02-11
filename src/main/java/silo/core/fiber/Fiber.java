/*
 *
 *  Copyright 2014 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.core.fiber;

import silo.lang.ExecutionContext;
import silo.lang.Function;

public class Fiber {

    // TODO: Fiber should accept a list of arguments that can stored

    public ExecutionContext context;
    public Function function;
    public Object[] arguments;

    public Object value;
    public Object resumedArgument;

    public Fiber(Function function, Object ... arguments) {
        this.context = new ExecutionContext();
        this.context.currentFiber = this;

        this.function = function;
        this.arguments = arguments;

        this.value = null;
        this.resumedArgument = null;
    }
}