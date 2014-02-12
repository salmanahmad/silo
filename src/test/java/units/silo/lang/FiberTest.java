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

import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

public class FiberTest {

    @Test
    public void testYield() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/fiber-test/yield.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        ExecutionContext fiber = new ExecutionContext();

        Class a = runtime.loader.loadClass("a");

        Assert.assertEquals(null, runtime.doEval(a, fiber));
        Assert.assertEquals("a", runtime.doEval(a, fiber));
    }

    @Test
    public void testFiber() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/fiber-test/fiber.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        String start = "start\nbefore yield\n";
        String finish = "finish\nbefore yield\nafter yield\nend of finish\n";

        PrintStream oldOut = System.out;

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            System.setOut(ps);

            runtime.eval("start");
            Assert.assertEquals(start, os.toString());



            os = new ByteArrayOutputStream();
            ps = new PrintStream(os);
            System.setOut(ps);

            runtime.eval("finish");
            Assert.assertEquals(finish, os.toString());
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    public void testGenerator() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/fiber-test/generator.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = runtime.eval("test");
        Vector vector = (Vector)o;
        Assert.assertEquals("first", vector.get(0));
        Assert.assertEquals("second", vector.get(1));
    }
}


