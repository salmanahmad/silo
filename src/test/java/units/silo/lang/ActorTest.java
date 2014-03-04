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

import org.apache.commons.lang3.StringUtils;

import com.github.krukow.clj_lang.PersistentVector;

public class ActorTest {

    @Test
    public void testSimple() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/actor-test/simple.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Actor bar = runtime.spawn("bar");
        Assert.assertEquals("You said: Hello!", bar.await());

        // Check that await does not block again...
        Assert.assertEquals("You said: Hello!", bar.await());
    }

    @Test
    public void testPingPong() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/actor-test/pingpong.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Actor bar = runtime.spawn("main");
        Assert.assertEquals(110, bar.await());
    }

    @Test
    public void testYield() throws Exception {
        Runtime runtime = null;
        String source = null;

        ByteArrayOutputStream os = null;
        PrintStream ps = null;

        PrintStream oldOut = System.out;

        try {
            os = new ByteArrayOutputStream();
            ps = new PrintStream(os);
            System.setOut(ps);

            runtime = new Runtime(new RuntimeClassLoader(), 1);
            source = Helper.readResource("/actor-test/yield-case-1.silo");
            runtime.compile(Parser.parse(source));
            runtime.spawn("main").await();
            Assert.assertEquals("main\nmain\nmain\nmain\nfoo\nfoo\nfoo\nfoo\n", os.toString());



            os = new ByteArrayOutputStream();
            ps = new PrintStream(os);
            System.setOut(ps);

            runtime = new Runtime(new RuntimeClassLoader(), 1);
            source = Helper.readResource("/actor-test/yield-case-2.silo");
            runtime.compile(Parser.parse(source));
            runtime.spawn("main").await();
            Assert.assertEquals("main\nfoo\nmain\nfoo\nmain\nfoo\nmain\nfoo\n", os.toString());
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    public void testArgs() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/actor-test/args.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Assert.assertEquals("You said: Hello, World!", runtime.spawn("main").await());
    }

    @Test
    public void testActorExceptions() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/actor-test/exceptions.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        ByteArrayOutputStream os = null;
        PrintStream ps = null;

        PrintStream oldErr = System.err;

        try {
            os = new ByteArrayOutputStream();
            ps = new PrintStream(os);
            System.setErr(ps);

            runtime.spawn("main").await();
            Assert.assertTrue(os.toString().length() != 0);
            Assert.assertTrue(StringUtils.startsWith(os.toString(), "Error: actor"));
            Assert.assertEquals(StringUtils.split(os.toString(), "\n")[1], "java.lang.RuntimeException: FooBar");
        } finally {
            System.setErr(oldErr);
        }
    }

    @Test
    public void testDeliveryDelayedRead() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/actor-test/delivery-delayed-read.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Actor a = runtime.spawn("main");

        for(int i = 0; i < 100; i++) {
            a.inboxPut("Message: " + i);
        }

        Assert.assertEquals("Message: 99", a.await());
    }

    @Test
    public void testDeliveryDelayedSend() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/actor-test/delivery-delayed-send.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Actor a = runtime.spawn("main");

        Thread.sleep(1000);

        for(int i = 0; i < 100; i++) {
            a.inboxPut("Message: " + i);
        }

        Assert.assertEquals("Message: 99", a.await());
    }

    @Test
    public void testSelectiveRecieve() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/actor-test/selective-receive.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Actor a = runtime.spawn("main");

        Thread.sleep(500);
        a.inboxPut(new Integer(0));
        Thread.sleep(500);
        a.inboxPut(new Integer(0));
        Thread.sleep(500);
        a.inboxPut(new Integer(0));
        Thread.sleep(500);
        a.inboxPut("FooBar");

        Assert.assertEquals("Value: FooBar", a.await());
    }
}


