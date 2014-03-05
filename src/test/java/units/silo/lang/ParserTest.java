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
    public void testMultiLineString() {
        String source = Helper.readResource("/examples/multiline-string.silo");
        Node program = Parser.parse(source);

        String string = "\n";
        string += "a\n";
        string += " b\n";
        string += "  c\n";
        string += "   d\n";
        string += "   e\n";
        string += "  f\n";
        string += " g\n";
        string += "h\n";

        Node expected = new Node(null, new Node(
            new Symbol("="),
            new Symbol("s"),
            string
        ));

        Assert.assertEquals(expected, program);
    }

    @Test
    public void testLineNumbers() {
        String source = Helper.readResource("/parser-test/line-numbers.silo");
        Node p = (Node)Parser.parse("line-numbers.silo", source);

        Node a = (Node)p.getFirstChild();
        Node b = (Node)a.getFirstChild();
        Node c = (Node)b.getFirstChild();
        Node d = (Node)c.getChild(0);
        Node e = (Node)c.getChild(1);
        Node f = (Node)c.getChild(2);

        Node math = (Node)p.getChild(1);

        Assert.assertEquals("line-numbers.silo", p.getMeta().valAt("file"));
        Assert.assertEquals(1, a.getMeta().valAt("line"));
        Assert.assertEquals(0, a.getMeta().valAt("position"));

        Assert.assertEquals(2, b.getMeta().valAt("line"));
        Assert.assertEquals(4, b.getMeta().valAt("position"));

        Assert.assertEquals(3, c.getMeta().valAt("line"));
        Assert.assertEquals(8, c.getMeta().valAt("position"));

        Assert.assertEquals(3, d.getMeta().valAt("line"));
        Assert.assertEquals(10, d.getMeta().valAt("position"));

        Assert.assertEquals(3, e.getMeta().valAt("line"));
        Assert.assertEquals(15, e.getMeta().valAt("position"));

        Assert.assertEquals(3, f.getMeta().valAt("line"));
        Assert.assertEquals(20, f.getMeta().valAt("position"));

        Assert.assertEquals(7, math.getMeta().valAt("line"));

        Node program = (Node)Parser.parse(source);
        Assert.assertEquals(null, program.getMeta().valAt("file"));
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
        e = new Node(null, new Node(new Symbol("fn"), new Symbol("a"), new Symbol("..."), new Node(null)));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a ... => int {})");
        e = new Node(null, new Node(new Symbol("fn"), new Symbol("a"), new Node(new Symbol("=>"), new Symbol("..."), new Symbol("int")), new Node(null)));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a, ... {})");
        e = new Node(null, new Node(new Symbol("fn"), new Symbol("a"), new Symbol("..."), new Node(null)));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a, ... => int {})");
        e = new Node(null, new Node(new Symbol("fn"), new Symbol("a"), new Node(new Symbol("=>"), new Symbol("..."), new Symbol("int")), new Node(null)));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a : int ... {})");
        e = new Node(null, new Node(new Symbol("fn"), new Node(new Symbol(":"), new Symbol("a"), new Symbol("int")), new Symbol("..."), new Node(null)));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a : int ... => int {})");
        e = new Node(null, new Node(new Symbol("fn"), new Node(new Symbol(":"), new Symbol("a"), new Symbol("int")), new Node(new Symbol("=>"), new Symbol("..."), new Symbol("int")), new Node(null)));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a : int, ... {})");
        e = new Node(null, new Node(new Symbol("fn"), new Node(new Symbol(":"), new Symbol("a"), new Symbol("int")), new Symbol("..."), new Node(null)));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(a : int, ... => int {})");
        e = new Node(null, new Node(new Symbol("fn"), new Node(new Symbol(":"), new Symbol("a"), new Symbol("int")), new Node(new Symbol("=>"), new Symbol("..."), new Symbol("int")), new Node(null)));
        Assert.assertEquals(e, n);
    }

    @Test
    public void testReturnTypes() {
        Node n = null;
        Node e = null;

        n = Parser.parse("fn(null => String {})");
        e = new Node(null, new Node(new Symbol("fn"), new Node(new Symbol("=>"), null, new Symbol("String")), new Node(null)));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(void => String {})");
        e = new Node(null, new Node(new Symbol("fn"), new Node(new Symbol("=>"), new Symbol("void"), new Symbol("String")), new Node(null)));
        Assert.assertEquals(e, n);

        n = Parser.parse("fn(=> String {})");
        e = new Node(null, new Node(new Symbol("fn"), new Symbol("=>"), new Symbol("String"), new Node(null)));
        Assert.assertEquals(e, n);
    }

    @Test
    public void testPrimitives() {
        Assert.assertEquals(new Long(5), Parser.parse("5L").getFirstChild());
        Assert.assertEquals(new Long(5), Parser.parse("5l").getFirstChild());
        Assert.assertEquals(new Integer(5), Parser.parse("5").getFirstChild());
        Assert.assertEquals(new Float(5), Parser.parse("5.0F").getFirstChild());
        Assert.assertEquals(new Float(5), Parser.parse("5.0f").getFirstChild());
        Assert.assertEquals(new Float(5), Parser.parse("5F").getFirstChild());
        Assert.assertEquals(new Float(5), Parser.parse("5f").getFirstChild());
        Assert.assertEquals(new Double(5), Parser.parse("5.0").getFirstChild());
    }

    @Test
    public void testLeftAssociativityOfHashAndPipe() {
        Node expected = null;

        expected = new Node(null,
            new Node(new Symbol("|"),
                new Node(new Symbol("|"),
                    new Node(new Symbol("|"),
                        new Symbol("a"),
                        new Symbol("b")
                    ),
                    new Symbol("c")
                ),
                new Symbol("d")
            )
        );

        Assert.assertEquals(expected, Parser.parse("a | b | c | d"));
        Assert.assertEquals(expected, Parser.parse("a|b|c|d"));

        expected = new Node(null,
            new Node(new Symbol("#"),
                new Node(new Symbol("#"),
                    new Node(new Symbol("#"),
                        new Symbol("a"),
                        new Symbol("b")
                    ),
                    new Symbol("c")
                ),
                new Symbol("d")
            )
        );

        Assert.assertEquals(expected, Parser.parse("a # b # c # d"));
        Assert.assertEquals(expected, Parser.parse("a#b#c#d"));

        expected = new Node(null,
            new Node(new Symbol("#"),
                new Node(new Symbol("#"),
                    new Node(new Symbol("#"),
                        new Symbol("o"),
                        new Node(new Symbol("a"))
                    ),
                    new Node(new Symbol("b"))
                ),
                new Node(new Symbol("c"))
            )
        );

        Assert.assertEquals(expected, Parser.parse("o#a()#b()#c()"));

    }
}
