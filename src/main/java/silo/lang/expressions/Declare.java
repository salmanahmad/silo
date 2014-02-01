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

import java.util.Vector;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

public class Declare implements Expression {

    public Declare(Node node) {
        throw new RuntimeException("Unimplemented");
    }

    public Class type(CompilationContext context) {
        throw new RuntimeException("Unimplemented");
    }

    public Object scaffold(CompilationContext context) {
        throw new RuntimeException("Unimplemented");
    }

    public void emit(CompilationContext context) {
        throw new RuntimeException("Unimplemented");
    }
}
