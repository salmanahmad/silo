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

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

public class DefineClassTest {
    @Test
    public void testFields() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/define-class-test/fields.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Assert.assertEquals("hello, world!", runtime.eval(classes.get(1)));
    }

    @Test
    public void testMethods() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/define-class-test/methods.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Assert.assertEquals("Hello, World!", runtime.eval(classes.get(1)));
        Assert.assertEquals("Hello, World!", runtime.eval(classes.get(2)));
        Assert.assertEquals("Hello, World!", runtime.eval(classes.get(3)));
        Assert.assertEquals(new Integer(10), runtime.eval(classes.get(4)));
        Assert.assertEquals(PersistentVectorHelper.create(new Integer(5), new Integer(10)), runtime.eval(classes.get(5)));
        Assert.assertEquals(PersistentVectorHelper.create(new Integer(30), new Integer(10)), runtime.eval(classes.get(6)));
    }

    @Test
    public void testInheritance() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/define-class-test/inheritance.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Assert.assertEquals(PersistentVectorHelper.create(
            "Vehicle Driving.",
            "Car Driving.",
            "Truck Driving."
        ), runtime.eval(classes.get(3)));

        Assert.assertEquals(PersistentVectorHelper.create(
            "Vehicle Driving.",
            "Car Driving.",
            "Truck Driving."
        ), runtime.eval(classes.get(4)));

        Assert.assertEquals(PersistentVectorHelper.create(
            "Vehicle Repairing.",
            "Vehicle Repairing.",
            "Vehicle Repairing."
        ), runtime.eval(classes.get(5)));

        Assert.assertEquals(PersistentVectorHelper.create(
            "Vehicle Repairing.",
            "Vehicle Repairing.",
            "Vehicle Repairing."
        ), runtime.eval(classes.get(5)));

        Assert.assertEquals(PersistentVectorHelper.create(
            "Vehicle Accelerate.",
            "Car Accelerate.",
            "Vehicle Accelerate."
        ), runtime.eval(classes.get(6)));
    }

    @Test
    public void testYieldStatic() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/define-class-test/yield-static.silo");
        runtime.compile(Parser.parse(source));

        IPersistentVector vector = PersistentVectorHelper.create("Waiting", "Result");
        Assert.assertEquals(vector, runtime.spawn("main").await());
    }

    @Test
    public void testYieldVirtual() {
        Runtime runtime = new Runtime();
        String source = Helper.readResource("/define-class-test/yield-virtual.silo");
        runtime.compile(Parser.parse(source));

        IPersistentVector vector = PersistentVectorHelper.create("Waiting", "This is a Result");
        Assert.assertEquals(vector, runtime.spawn("main").await());
    }

    @Test
    public void testResumabilityEnforcement() {
        Runtime runtime = new Runtime();
        String source = null;

        source = Helper.readResource("/define-class-test/resumability-enforcement-good.silo");
        runtime.compile(Parser.parse(source));

        try {
            source = Helper.readResource("/define-class-test/resumability-enforcement-bad.silo");
            runtime.compile(Parser.parse(source));
             Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
            Assert.assertEquals("Cannot perform resumable call from a non-resumable context.", e.getMessage());
        }

        try {
            source = Helper.readResource("/define-class-test/resumability-enforcement-default.silo");
            runtime.compile(Parser.parse(source));
             Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
            Assert.assertEquals("Cannot perform resumable call from a non-resumable context.", e.getMessage());
        }
    }
}


