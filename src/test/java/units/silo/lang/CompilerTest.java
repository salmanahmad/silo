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


import org.junit.*;
import java.util.*;

import silo.lang.Runtime;
import silo.lang.*;
import silo.lang.compiler.*;
import silo.lang.compiler.Compiler;
import silo.lang.compiler.grammar.*;

import org.objectweb.asm.Type;

public class CompilerTest {

    @Test
    public void testSimple() {
        Runtime runtime = new Runtime();

        Node program = Parser.parse("print(5 + 6)");
        java.util.Vector<Class> classes = runtime.compile(program);

        try {
            Class klass = classes.get(0);
            ((Function)klass.newInstance()).methodHandle().invoke(null);
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error!");
        }
    }
}
