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

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

import org.objectweb.asm.Type;

public class CompilerTest {

    @Test
    public void testSimple() {
        Runtime runtime = new Runtime();

        PrintStream oldOut = System.out;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        System.setOut(ps);

        try {
            Object o = runtime.eval(Parser.parse("invokevirtual(System.out, println(5 + 5))"));
            Assert.assertEquals(null, o);
            Assert.assertEquals("10\n", os.toString());
        } finally {
            System.setOut(oldOut);
        }
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

        PrintStream oldOut = System.out;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        System.setOut(ps);

        try {
            Object o = runtime.eval(classes.get(0));
            Assert.assertEquals(null, o);
            Assert.assertEquals("Hello, World!\n", os.toString());
        } finally {
            System.setOut(oldOut);
        }
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

        o = runtime.eval(classes.get(1));
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
    public void testBranchSingle() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/branch-single.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Assert.assertEquals(3, runtime.eval(classes.get(0)));
        Assert.assertEquals(3, runtime.eval(classes.get(1)));
        Assert.assertEquals(3, runtime.eval(classes.get(2)));
        Assert.assertEquals(3, runtime.eval(classes.get(3)));
    }

    @Test
    public void testBranchBoxing() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/branch-boxing.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Object o = null;

        Assert.assertEquals(5, runtime.eval(classes.get(0)));
        Assert.assertEquals(10.0, runtime.eval(classes.get(1)));
        Assert.assertEquals(null, runtime.eval(classes.get(2)));
        Assert.assertEquals(null, runtime.eval(classes.get(3)));
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
             Assert.fail();
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
             Assert.fail();
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

        PrintStream oldOut = System.out;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        System.setOut(ps);

        try {
            Object o = runtime.eval(classes.get(0));
            Assert.assertEquals("FooBars", o);
            Assert.assertEquals("FooBars\n", os.toString());
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    public void testWhile() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/while.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = runtime.eval(classes.get(0));

        Assert.assertEquals(5, o);
    }

    @Test
    public void testArrayType() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/array-type.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = null;

        o = runtime.eval(classes.get(0));
        Assert.assertEquals(int[].class, o);

        o = runtime.eval(classes.get(1));
        Assert.assertEquals(Integer[].class, o);

        o = runtime.eval(classes.get(2));
        Assert.assertEquals(int[][][][].class, o);

        o = runtime.eval(classes.get(3));
        Assert.assertEquals(int[][][][].class, o);
    }

    @Test
    public void testArrayNew() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/array-new.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = null;

        o = runtime.eval(classes.get(0));
        int[] a = new int[4];
        int[] b = (int[])o;

        Assert.assertTrue(java.util.Arrays.equals(a, b));
        Assert.assertEquals(b[0], 0);
        Assert.assertEquals(b[1], 0);
        Assert.assertEquals(b[2], 0);
        Assert.assertEquals(b[3], 0);
        Assert.assertEquals(b.length, 4);
    }

    @Test
    public void testArray() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/array.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = null;

        o = runtime.eval(classes.get(1));
        Assert.assertEquals(new Integer(5), o);
    }

    @Test
    public void testTypePropogation() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/math-test-propogation.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = null;

        o = runtime.eval(classes.get(0));
        Assert.assertEquals(Integer.MAX_VALUE - 5.0, o);
    }

    @Test
    public void testVarArgConstructor() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/varargs-constructor.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = null;

        o = runtime.eval(classes.get(0));
        Assert.assertEquals(new Node(null, "hello", "world", "this", "is", "fun"), o);
    }

    @Test
    public void testVarArgStatic() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/varargs-static.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = null;

        Assert.assertEquals(Arrays.asList("Hello", null, "Hi!", null, "Welcome!"), runtime.eval(classes.get(0)));
        Assert.assertEquals(Arrays.asList(), runtime.eval(classes.get(1)));
    }

    @Test
    public void testVarArgVirtual() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/varargs-virtual.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = null;

        o = runtime.eval(classes.get(0));

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        formatter.format("%4$2s %3$2s %2$2s %1$2s", "a", "b", "c", "d");

        Assert.assertEquals(sb.toString(), o);
    }

    @Test
    public void testVarArg() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/varargs.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = null;

        o = runtime.eval(classes.get(1));
        Assert.assertEquals("[\"a\" \"b\" \"c\" \"d\" \"e\" \"f\"]", o);
    }

    @Test
    public void testNestedVariables() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/nested-variables.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Object o = null;

        Assert.assertEquals("Hello, World!", runtime.eval(classes.get(0)));
        Assert.assertEquals(null, runtime.eval(classes.get(1)));
        Assert.assertEquals("bar bar bar", runtime.eval(classes.get(2)));
    }

    @Test
    public void testNullAssignment() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/null-assignment.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals(null, runtime.eval(classes.get(0)));
        Assert.assertEquals(null, runtime.eval(classes.get(2)));
    }

    @Test
    public void testDefaultAliases() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/default-aliases.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals(PersistentVector.emptyVector(), runtime.eval(classes.get(0)));
    }

    @Test
    public void testAlias() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/alias.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals(new java.util.Date(1, 1, 1), runtime.eval(classes.get(0)));
    }

    @Test
    public void testInstanceof() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/instanceof.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals(true, runtime.eval(classes.get(0), new Integer(0)));
        Assert.assertEquals(false, runtime.eval(classes.get(0), new Float(0)));
    }

    @Test
    public void forwardDeclaration() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/forward-declaration.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals("Hello, World!", runtime.eval(classes.get(0)));
    }

    @Test
    public void recursion() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/recursion.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals(new Integer(39916800), runtime.eval(classes.get(0), new Integer(11)));
    }

    @Test
    public void mutualRecursion() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/mutual-recursion.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals(true, runtime.eval(classes.get(0), new Integer(10)));
    }

    @Test
    public void forwardDeclareMacro() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/macro-forward-declare.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals("Hello, Macro!", runtime.eval(classes.get(1)));
    }

    @Test
    public void nullCheck() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/null-check.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals(false, runtime.eval("foo"));
        Assert.assertEquals(true, runtime.eval("bar"));
        Assert.assertEquals(true, runtime.eval("bar0"));
    }

    @Test
    public void testArrayLength() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/array-length.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals(" 0 1 2 3 4 5", runtime.eval("join", new int[] {0, 1, 2, 3, 4, 5}));
    }

    @Test
    public void testThrow() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/throw.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));

        try {
             runtime.eval(classes.get(0));
             Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
            Assert.assertEquals("", e.getMessage());
        }

        try {
             runtime.eval(classes.get(1));
             Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
            Assert.assertEquals("", e.getMessage());
        }

        try {
             runtime.eval(classes.get(2));
             Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
            Assert.assertEquals("Exception!", e.getMessage());
        }

        try {
             runtime.eval(classes.get(3));
             Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
            Assert.assertEquals("5", e.getMessage());
        }

        try {
             runtime.eval(classes.get(4));
             Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
            Assert.assertEquals("false", e.getMessage());
        }

        try {
             runtime.eval(classes.get(5));
             Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
            Assert.assertEquals("FooBar!", e.getMessage());
        }

        try {
             runtime.eval(classes.get(6));
             Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(NullPointerException.class, e.getClass());
            Assert.assertEquals("Null!", e.getMessage());
        }
    }

    @Test
    public void testCatch() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/catch.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals("Hello!", runtime.eval(classes.get(0)));
        Assert.assertEquals(false, runtime.eval(classes.get(1)));
        Assert.assertEquals("Exception!", runtime.eval(classes.get(2)));
        Assert.assertEquals("Exception!", runtime.eval(classes.get(3)));
    }

    @Test
    public void testFinallyDuplicatedDefinitions() {
        Runtime runtime = new Runtime();
        String good = Helper.readResource("/examples/finally-good.silo");
        String bad = Helper.readResource("/examples/finally-bad.silo");

        runtime.compile(Parser.parse(good));

        try {
            runtime.compile(Parser.parse(bad));
            Assert.fail();
        } catch(RuntimeException e) {
            Assert.assertEquals("Attempting to re-define a function named: foo", e.getMessage());
        }
    }

    @Test
    public void testTry() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/try.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals("Hello!", runtime.eval(classes.get(0)));
        Assert.assertEquals(false, runtime.eval(classes.get(1)));
        Assert.assertEquals("Exception!", runtime.eval(classes.get(2)));
        Assert.assertEquals("Exception!", runtime.eval(classes.get(3)));
    }

    @Test
    public void testTryFinally() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/try-finally.silo");

        Vector<Class> classes = runtime.compile(Parser.parse(source));
        Assert.assertEquals("Finally, World!", runtime.eval(classes.get(0)));
        Assert.assertEquals("Error: ClassCast", runtime.eval(classes.get(1)));
        Assert.assertEquals("Error: NullPointer", runtime.eval(classes.get(2)));
        Assert.assertEquals("Error: IndexOutOfBounds", runtime.eval(classes.get(3)));
        Assert.assertEquals("Message: ClassCast", runtime.eval(classes.get(4)));
        Assert.assertEquals("Message: NullPointer", runtime.eval(classes.get(5)));
        Assert.assertEquals("Message: IndexOutOfBounds", runtime.eval(classes.get(6)));
    }

    @Test
    public void testLineNumbers() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/line-numbers.silo");
        Vector<Class> classes = runtime.compile(Parser.parse("line-numbers.silo", source));

        try {
            runtime.eval(classes.get(0));
            Assert.fail();
        } catch(RuntimeException e) {
            Assert.assertEquals("A new world!", e.getMessage());
            Assert.assertEquals(7, e.getStackTrace()[0].getLineNumber());
            Assert.assertEquals("line-numbers.silo", e.getStackTrace()[0].getFileName());
        }

        classes = runtime.compile(Parser.parse("/examples/line-numbers.silo", source));

        try {
            runtime.eval(classes.get(0));
            Assert.fail();
        } catch(RuntimeException e) {
            Assert.assertEquals("/examples/line-numbers.silo", e.getStackTrace()[0].getFileName());
        }

        classes = runtime.compile(Parser.parse("line-numbers.silo", source));

        try {
            runtime.eval(classes.get(1));
            Assert.fail();
        } catch(RuntimeException e) {
            Assert.assertEquals("A new world!", e.getMessage());
            Assert.assertEquals(14, e.getStackTrace()[0].getLineNumber());
            Assert.assertEquals("line-numbers.silo", e.getStackTrace()[0].getFileName());
        }
    }

    @Test
    public void testFunctionHandle() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/examples/function-handle.silo");
        Vector<Class> classes = runtime.compile(Parser.parse("function-handle.silo", source));

        Object o = runtime.eval("functionReference");
        Assert.assertTrue(Function.class.isAssignableFrom(o.getClass()));
        Assert.assertEquals(5, runtime.eval("functionCall"));
        Assert.assertEquals(11, runtime.eval("functionArgs"));
    }
}
