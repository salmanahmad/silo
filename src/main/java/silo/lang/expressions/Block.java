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

public class Block implements Expression {

    public final Vector<Expression> expressions;

    public Block(Vector<Expression> expressions) {
        this.expressions = expressions;
    }

    public void emit(CompilationContext context, GeneratorAdapter generator) {
        if(expressions != null) {
            for(Expression expression : expressions) {
                // TODO - When do I want to cascade the last value up the tree? Conditional statements and loops?

                int size = context.operandStack.size();

                expression.emit(context, generator);

                size = context.operandStack.size() - size;

                if(size > 1) {
                    // TODO - How do I handle multiple return values?
                    throw new RuntimeException("The operand stack should not change more than one for nodes.");
                }

                for(int i = 0; i < size; i++) {
                    // TODO - What if the value on the stack is a category2 type? I need to pop more than just 1, right?
                    context.operandStack.pop();
                    generator.pop();
                }
            }
        }
    }
}
