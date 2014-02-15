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

import silo.core.fiber.Fiber;
import silo.core.fiber.resume;

import silo.lang.Runtime;
import silo.lang.*;
import silo.lang.compiler.*;
import silo.lang.compiler.Compiler;
import silo.lang.compiler.grammar.*;

import org.objectweb.asm.Type;

import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

import com.github.krukow.clj_lang.PersistentVector;

public class FiberTest {

    @Test
    public void testYield() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/fiber-test/yield.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Fiber fiber = new Fiber((Function)runtime.loader.loadClass("a").newInstance());

        resume.invoke(fiber.context, fiber, PersistentVector.emptyVector());
        Assert.assertEquals(null, fiber.value);

        resume.invoke(fiber.context, fiber, PersistentVector.emptyVector());
        Assert.assertEquals("a", fiber.value);
    }

    @Test
    public void testFiber() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/fiber-test/fiber.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        String start = "start\nbefore yield\n";
        String finish = "finish\nbefore yield\nafter yield\nend of finish\n";

        PrintStream oldOut = System.out;

        Fiber fiber = null;

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            System.setOut(ps);

            fiber = new Fiber((Function)runtime.loader.loadClass("start").newInstance());
            resume.invoke(fiber.context, fiber, PersistentVector.emptyVector());
            Assert.assertEquals(start, os.toString());



            os = new ByteArrayOutputStream();
            ps = new PrintStream(os);
            System.setOut(ps);

            fiber = new Fiber((Function)runtime.loader.loadClass("finish").newInstance());
            resume.invoke(fiber.context, fiber, PersistentVector.emptyVector());
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

        Fiber fiber = new Fiber((Function)runtime.loader.loadClass("test").newInstance());
        resume.invoke(fiber.context, fiber, PersistentVector.emptyVector());

        Vector vector = (Vector)fiber.value;
        Assert.assertEquals("first", vector.get(0));
        Assert.assertEquals("second", vector.get(1));
    }
}


