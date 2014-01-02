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
        runtime.eval(Parser.parse("invokevirtual(System.out, println(5 + 5))"));
    }

    @Test
    public void testFunctionImplicitReturns() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/function-implicit-returns.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = runtime.eval(classes.get(0));

        Assert.assertEquals(new Integer(95), o);
    }

    @Test
    public void testFunctionExplicitReturns() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/function-explicit-returns.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = runtime.eval(classes.get(0));

        Assert.assertEquals(new Integer(42), o);
    }

    @Test
    public void testSimpleVariables() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/variables.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        for(int i = 0; i < classes.size() - 1; i++) {
            Object o = runtime.eval(classes.get(i));
            Assert.assertEquals(new Integer(25 + i), o);
        }
    }

    @Test
    public void testInvokeVirtual() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/invokevirtual.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = runtime.eval(classes.get(0));
    }

    @Test
    public void testFunctionCall() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/function-call.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = runtime.eval(classes.get(1));
        Assert.assertEquals(new Integer(15), o);
    }

    @Test
    public void testMath() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/math-test.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = runtime.eval(classes.get(0));
        Assert.assertEquals(new Double(95.5), o);
    }

    @Test
    public void testBranch() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/branch.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = runtime.eval(classes.get(0));
        Assert.assertEquals(new Integer(5), o);

        o = runtime.eval(classes.get(1));
        Assert.assertEquals(new Integer(10), o);

        o = runtime.eval(classes.get(1));
        Assert.assertEquals(new Integer(10), o);
    }

    @Test
    public void testBranchReturns() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/branch-returns.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = runtime.eval(classes.get(0));
        Assert.assertEquals(new Integer(5), o);
    }

    @Test
    public void testRelationalOperators() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/relational.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = runtime.eval(classes.get(0));
        Assert.assertEquals(new Boolean(true), o);

        o = runtime.eval(classes.get(1));
        Assert.assertEquals(new Boolean(false), o);
    }

    @Test
    public void testArguments() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/arguments.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = runtime.eval(classes.get(1));
        Assert.assertEquals(new Integer(50), o);

        o = runtime.eval(classes.get(0), new Object[] {104});
        Assert.assertEquals(new Integer(109), o);
    }

    @Test
    public void testLoop() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/loop-test.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = runtime.eval(classes.get(0));
        Assert.assertEquals(new Integer(10), o);
    }

    @Test
    public void testLogicalOperators() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/logical-operators.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = runtime.eval(classes.get(0));
        Assert.assertEquals(new Boolean(true), o);

        o = runtime.eval(classes.get(2), new Boolean(true));
        Assert.assertEquals(new Boolean(true), o);
    }

    @Test
    public void testTypes() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/types.silo");

        try {
             Vector<Class> classes = runtime.compile(Parser.parse(source));
             Object o = runtime.eval(classes.get(0));
        } catch (Exception e) {
            Assert.assertEquals(e.toString(), "java.lang.RuntimeException: Invalid assignment from type double to int");
        }
    }

    @Test
    public void testInvalidParameters() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/invalid-parameters.silo");

        try {
             Vector<Class> classes = runtime.compile(Parser.parse(source));
             Object o = runtime.eval(classes.get(0));
        } catch (Exception e) {
            Assert.assertEquals(e.toString(), "java.lang.RuntimeException: Parameter mismatch in class foo. Expected: int Provided: double");
        }
    }

    @Test
    public void testConstructor() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/constructor.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = runtime.eval(classes.get(0));

        Assert.assertEquals("baz", o);
    }

    @Test
    public void testStdLib() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/stdlib.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = runtime.eval(classes.get(0));

        Assert.assertEquals("FooBars", o);
    }

    @Test
    public void testWhile() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/while.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = runtime.eval(classes.get(0));

        Assert.assertEquals(5, o);
    }
}
