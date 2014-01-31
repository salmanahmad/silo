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

public class Import implements Expression {

    public final Node node;

    public Import(Node node) {
        this.node = node;
    }

    public Class type(CompilationContext context) {
        return Null.class;
    }

    public void emitDeclaration(CompilationContext context) {
        // TODO: Refactor this with emit Below...

        Vector<Symbol> list = Compiler.symbolList(node.getFirstChild());
        if(list != null) {
            String i = StringUtils.join(list, ".");
            context.currentNamespace().imports.add(i);
        } else {
            throw new RuntimeException("Invalid import declaration");
        }
    }

    public void emit(CompilationContext context) {
        CompilationFrame frame = context.currentFrame();
        GeneratorAdapter generator = frame.generator;

        // TODO: How do I differentiate `java.util` and `java.util.Date`.
        // One should be an import and the other should be an alias for
        // `Date` => `java.util.Date`. Perhaps I do not worry about that
        // here. If you want that functionality, then create a macro that
        // will rewrite to import or alias...

        Vector<Symbol> list = Compiler.symbolList(node.getFirstChild());
        if(list != null) {
            String i = StringUtils.join(list, ".");
            context.currentNamespace().imports.add(i);
        } else {
            throw new RuntimeException("Invalid import declaration");
        }

        generator.push((String)null);
        frame.operandStack.push(Null.class);
    }
}
