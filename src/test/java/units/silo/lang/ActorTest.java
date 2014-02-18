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
}


