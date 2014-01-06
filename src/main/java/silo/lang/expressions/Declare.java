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

    public static Declare build(Node node) {
        return null;
    }

    public Declare() {
        
    }

    public Class type(CompilationContext context) {
        throw new RuntimeException("Unimplemented");
    }

    public void emit(CompilationContext context) {
        throw new RuntimeException("Unimplemented");
    }
}
