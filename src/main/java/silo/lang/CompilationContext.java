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

import org.objectweb.asm.Type;

import java.util.Stack;

public class CompilationContext {

    public Stack<Type> operandStack = new Stack<Type>();

}
