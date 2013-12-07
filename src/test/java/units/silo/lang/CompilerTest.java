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

import silo.lang.*;
import silo.lang.compiler.*;
import silo.lang.compiler.Compiler;
import silo.lang.compiler.grammar.*;

public class CompilerTest {

    @Test
    public void testSimple() {
        Parser parser = new Parser();
        Node program = parser.parse("print(5 + 4)");

        Compiler compiler = new Compiler(new RuntimeClassLoader());
        compiler.compile(program);
    }
}
