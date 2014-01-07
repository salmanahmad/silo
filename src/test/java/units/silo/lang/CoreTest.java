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

public class CoreTest {

    @Test
    public void testFn() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/core-test/fn.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = runtime.eval(classes.get(0));
        System.out.println(o);
    }
}
