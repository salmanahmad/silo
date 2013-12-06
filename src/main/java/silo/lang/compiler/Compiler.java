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

package silo.lang.compiler;

import silo.lang.*;

import java.util.Vector;

public class Compiler {
    public RuntimeClassLoader classloader;

    public Compiler(RuntimeClassLoader classloader) {
        this.classloader = classloader;
    }

    public Vector<Node> compile(Node node) {
        /*
        node = expandMacros(node);

        Vector<Symbol> declarations = extractDeclarations(node);
        checkForDuplicates(declarations);

        Vector<Node> containsUncompiledMacros(node)



        Object expressionTree = transformToExpressions(node);

        emit(expressionTree);
        */
        
        return null;
    }

    public Object expandMacros(Object node) {
        /*
        Object previous = null;

        while(true) {
            previous = node;
            node = expandMacrosOnce(node);

            if(previous.equals(node)) {
                break;
            }
        }

        return node;
        */
        return null;
    }

    private void checkForDuplicates(Vector<Node> declarations) {
        
    }

    public Object expandMacrosOnce(Object node) {
        // TODO
        return null;
    }

    private boolean containsUncompiledMacros(Object node) {
        return false;
    }

    public Vector<Node> extractDeclarations(Node node) {
        return null;
    }

    public Object transformToExpressions(Object node) {
        return null;
    }

    public void emit(Node node) {
        
    }
}



