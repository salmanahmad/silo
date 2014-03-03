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
        Assert.assertEquals(10, runtime.eval(classes.get(0), 5, 5));
        Assert.assertEquals(6, runtime.eval(classes.get(1), 1, "a", "b", "c", "d", "e", "f"));
    }

    @Test
    public void testFunc() throws ClassNotFoundException {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/core-test/func.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Class main = runtime.loader.loadClass("silo.test.core.main");
        Assert.assertEquals(12, runtime.eval(main, 6, 6));
    }

    @Test
    public void testIf() throws ClassNotFoundException {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/core-test/if.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Class main = runtime.loader.loadClass("silo.test.core.fib");
        Assert.assertEquals(610, runtime.eval(main, 15));
    }

    @Test
    public void testSimpleBranch() throws ClassNotFoundException {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/core-test/simple-branch.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Class main = runtime.loader.loadClass("main");
        Assert.assertEquals(5, runtime.eval(main));
    }

    @Test
    public void testFuncWithOnlyOutput() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/core-test/only-output.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Assert.assertEquals("This is a cool string!", runtime.eval(classes.get(0)));
        Assert.assertEquals("This is a cool string!", runtime.eval("foo"));
        Assert.assertEquals("This is a cool string!", runtime.eval("bar"));
    }

    @Test
    public void testFuncWithOnlyOutputAlt() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/core-test/only-output-alt.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Assert.assertEquals("This is a cool str!", runtime.eval(classes.get(0)));
        Assert.assertEquals("This is a cool str!", runtime.eval("foo"));
        Assert.assertEquals("This is a cool str!", runtime.eval("bar"));
    }

    @Test
    public void testDefaultReturnType() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/core-test/default-return-type.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Assert.assertEquals("0", runtime.eval(classes.get(0)));
        Assert.assertEquals("1", runtime.eval(classes.get(1)));
        Assert.assertEquals("2", runtime.eval("foo"));
        Assert.assertEquals("3", runtime.eval("bar"));
        Assert.assertEquals("4", runtime.eval("foo0"));
        Assert.assertEquals("5", runtime.eval("bar0"));
        Assert.assertEquals("6", runtime.eval("bar1"));
    }

    @Test
    public void testTime() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/core-test/time.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = runtime.eval("test");
    }

    @Test
    public void testVector() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/core-test/vector.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Assert.assertEquals(2, runtime.eval("test"));
    }

    @Test
    public void testMap() throws Exception {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/core-test/map.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Assert.assertEquals("bar", runtime.eval("test"));
    }
}


