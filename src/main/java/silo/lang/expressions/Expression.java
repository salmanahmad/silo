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

import org.objectweb.asm.commons.GeneratorAdapter;

public interface Expression {
    public void emit(CompilationContext context);
    public Object scaffold(CompilationContext context);
    public Class type(CompilationContext context);

    // TODO - Understand the following comment and then remove it.
    // There are far-easier way to accomplish this...
    // TODO - Is this useful to include here? Could be nice to abstract
    // functionality that is ahred implicity between here and scaffold...
    //public Node macroexpand(CompilationContext context);
}
