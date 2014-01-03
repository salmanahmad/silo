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
import silo.lang.compiler.grammar.*;

public class ParserTest {

    @Test
    public void testResourceExamples() throws Exception {
        String[] paths = Helper.getResourceListing("/examples/");

        for(String path : paths) {
            path = "/examples/" + path;
            try {
                String source = Helper.readResource(path);
                Node program = Parser.parse(source);
                //System.out.println(program.toPrettyString());
            } catch(Exception e) {
                System.out.println("Failed on test: " + path);
                throw e;
            }
        }
    }

    @Test
    public void testExample() throws Exception {
        String path = System.getProperty("example");

        if(path != null) {
            try {
                String source = Helper.readResource("/examples/" + path);
                Node program = Parser.parse(source);
                System.out.println(program.toPrettyString());
            } catch(Exception e) {
                System.out.println("Failed on test: " + path);
                throw e;
            }
        }
    }

    @Test
    public void testLeftAssociativity() throws Exception {
        Node program = Parser.parse("a.b.c.d");

        Node expected = new Node(null, new Node(
            new Symbol("."),
            new Node(
                new Symbol("."),
                new Node(
                    new Symbol("."),
                    new Symbol("a"),
                    new Symbol("b")
                ),
                new Symbol("c")
            ),
            new Symbol("d")
        ));

        Assert.assertEquals(expected, program);
    }

    @Test
    public void testOptionalCommas() throws Exception {
        Node program = Parser.parse("a(x,,y z,,,,,)");

        Node expected = new Node(null, new Node(new Symbol("a"),
            new Symbol("x"),
            new Symbol("y"),
            new Symbol("z")
        ));

        Assert.assertEquals(expected, program);
    }

    @Test
    public void testEmpty() {
        Parser.parse("");
        Parser.parse("\n\n\n");
        Parser.parse("\n\n  \n \n");
    }

    @Test
    public void testSymbol() {
        Parser.parse("foo");
    }

    @Test
    public void testOptionalSpace() {
        Node program = Parser.parse("foo   ()");

        Node expected = new Node(null, new Node(new Symbol("foo")));

        Assert.assertEquals(expected, program);
    }

    @Test
    public void testChildlessNode() {
        Parser.parse("foo()");
    }

    @Test
    public void testOneChildNode() {
        Parser.parse("foo(bar)");
    }

    @Test
    public void testManyChildrenNode() {
        Parser.parse("foo(bar, baz, qux)");
    }

    @Test
    public void testNestedChildren() {
        Parser.parse("foo(bar(), baz, qux())");
    }

    @Test
    public void testOperatorsInIdentifiers() {
        Node n = Parser.parse("foo, ..., list* 5 +(1 2), 1 + 2");
        // TODO: Assert the proper structure of the tree...
    }

    @Test
    public void testVarArgs() {
        Node n = null;
        Node e = null;

        n = Parser.parse("fn(a ... {})");
        e = new Node(null, new Node(new Symbol("fn"), new Symbol("a"), new Symbol("..."), new Node(new Symbol("do"))));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a ... => int {})");
        e = new Node(null, new Node(new Symbol("fn"), new Symbol("a"), new Node(new Symbol("=>"), new Symbol("..."), new Symbol("int")), new Node(new Symbol("do"))));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a, ... {})");
        e = new Node(null, new Node(new Symbol("fn"), new Symbol("a"), new Symbol("..."), new Node(new Symbol("do"))));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a, ... => int {})");
        e = new Node(null, new Node(new Symbol("fn"), new Symbol("a"), new Node(new Symbol("=>"), new Symbol("..."), new Symbol("int")), new Node(new Symbol("do"))));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a : int ... {})");
        e = new Node(null, new Node(new Symbol("fn"), new Node(new Symbol(":"), new Symbol("a"), new Symbol("int")), new Symbol("..."), new Node(new Symbol("do"))));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a : int ... => int {})");
        e = new Node(null, new Node(new Symbol("fn"), new Node(new Symbol(":"), new Symbol("a"), new Symbol("int")), new Node(new Symbol("=>"), new Symbol("..."), new Symbol("int")), new Node(new Symbol("do"))));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a : int, ... {})");
        e = new Node(null, new Node(new Symbol("fn"), new Node(new Symbol(":"), new Symbol("a"), new Symbol("int")), new Symbol("..."), new Node(new Symbol("do"))));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a : int, ... => int {})");
        e = new Node(null, new Node(new Symbol("fn"), new Node(new Symbol(":"), new Symbol("a"), new Symbol("int")), new Node(new Symbol("=>"), new Symbol("..."), new Symbol("int")), new Node(new Symbol("do"))));
        Assert.assertEquals(e, n);
    }
}
