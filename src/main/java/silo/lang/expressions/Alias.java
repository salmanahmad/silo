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

import org.objectweb.asm.commons.GeneratorAdapter;
import org.apache.commons.lang3.StringUtils;

// TODO: Update Alias to take a block in which the changes to namespaces are visible. After the block, the namespaces are reverted. Similar to package.

public class Alias implements Expression {

    public final Node node;

    public Alias(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Null.class;
    }

    public Object scaffold(CompilationContext context) {
        // TODO: Refactor this method with emit below...
        String source = null;
        String target = null;

        Object o = node.getFirstChild();
        if(o instanceof Symbol) {
            source = o.toString();
        } else {
            throw new RuntimeException("Invalid alias declaration");
        }

        Vector<Symbol> list = Compiler.symbolList(node.getSecondChild());
        if(list != null) {
            target = StringUtils.join(list, ".");
        } else {
            throw new RuntimeException("Invalid alias declaration");
        }

        context.currentNamespace().aliases.put(source, target);

        return node;
    }

    public void emit(CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        String source = null;
        String target = null;

        Object o = node.getFirstChild();
        if(o instanceof Symbol) {
            source = o.toString();
        } else {
            throw new RuntimeException("Invalid alias declaration");
        }

        Vector<Symbol> list = Compiler.symbolList(node.getSecondChild());
        if(list != null) {
            target = StringUtils.join(list, ".");
        } else {
            throw new RuntimeException("Invalid alias declaration");
        }

        context.currentNamespace().aliases.put(source, target);

        generator.push((String)null);
        frame.operandStack.push(Null.class);
    }
}
