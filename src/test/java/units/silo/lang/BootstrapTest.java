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

public class BootstrapTest {

    @Test
    public void testMain() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/bootstrap-test/main.silo");

        //Vector<Class> classes = runtime.compile(Parser.parse(source));
        //System.out.println(runtime.eval(classes.get(4)));

        //CompilationContext context = runtime.contextByCompiling(source);
        //silo.lang.Compile.writeBytecodeToDirectory(context.bytecode, "/Users/salmanahmad/Desktop/bootstrap/");
    }
}


