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
import silo.lang.compiler.Compiler;

import java.util.Vector;

public class Block implements Expression {

    Node node;

    public Block(Node node) {
        this.node = node;
    }

    public Vector<Expression> expressions() {
        Vector<Expression> expressions = new Vector<Expression>();

        if(node.getChildren() != null) {
            for(Object child : node.getChildren()) {
                expressions.add(Compiler.buildExpression(child));
            }
        }

        return expressions;
    }

    public Class type(CompilationContext context) {
        Vector<Expression> expressions = expressions();

        if(expressions != null && expressions.size() > 0) {
            Expression e = expressions.get(expressions.size() - 1);
            return e.type(context);
        } else {
            return Null.class;
        }
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        Vector<Expression> expressions = expressions();

        CompilationFrame frame = context.currentFrame();

        if(expressions != null && expressions.size() > 0) {
            for(int i = 0; i < expressions.size(); i++) {
                Expression expression = expressions.get(i);

                // TODO - When do I want to cascade the last value up the tree? Conditional statements and loops?

                int size = frame.operandStack.size();

                expression.emit(context);

                size = frame.operandStack.size() - size;

                if(size > 1) {
                    // TODO - How do I handle multiple return values?
                    throw new RuntimeException("The operand stack should not change more than one for nodes. It changed by: " + size);
                }

                if(i < (expressions.size() - 1)) {
                    for(int j = 0; j < size; j++) {
                        // TODO - What if the value on the stack is a category2 type? I need to pop more than just 1, right?
                        Class operand = frame.operandStack.pop();
                        Compiler.pop(operand, frame.generator);
                    }
                } else {
                    if(size == 0) {
                        // TODO: This should be var.
                        frame.operandStack.push(Null.class);
                        frame.generator.push((String)null);
                    }
                }
            }
        } else {
            // TODO: Is this necessary here? Does this impact performance or code-size in any meaningful way?
            frame.operandStack.push(Null.class);
            frame.generator.push((String)null);
        }
    }
}
