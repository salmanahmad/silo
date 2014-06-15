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

package silo.lang.expressions;

import silo.lang.*;
import silo.lang.compiler.Compiler;
import silo.lang.compiler.Parser;

import java.util.Vector;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

public class Require implements Expression {

    public Node node;

    public Require(Node node) {
        this.node = node;

        if(!(node.getChildren().size() == 1)) {
            throw new RuntimeException("Require needs exactly one string argument.");
        }

        if(!(node.getFirstChild() instanceof String)) {
            throw new RuntimeException("Require needs exactly one string argument.");
        }
    }

    public Class type(CompilationContext context) {
        throw new RuntimeException("Require should not ever be called...");
    }

    public Object scaffold(CompilationContext context) {
        try {
            String file = (new File(node.getFirstChild().toString())).getCanonicalPath();
            if(context.files.contains(file)) {
                return null;
            } else {
                context.files.add(file);
                String source = FileUtils.readFileToString(new File(file));
                Node code = Parser.parse(file, source);
                return Compiler.buildExpression(code).scaffold(context);
            }
        } catch(IOException e) {
            return null;
        }
    }

    public void emit(CompilationContext context) {
        throw new RuntimeException("Require should not ever be called...");
    }
}
