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

public class NodeTest {

    @Test
    public void testSymbolEquality() {
        Symbol s1 = new Symbol("foo");
        Symbol s2 = new Symbol("foo");
        Symbol s3 = new Symbol("baz");
        Symbol s4 = new Symbol("baz");

        Assert.assertEquals(s1, s2);
        Assert.assertEquals(s3, s4);

        Assert.assertNotEquals(s1, s3);
        Assert.assertNotEquals(s1, s4);
    }

    @Test
    public void testEquality() {
        Node n1 = new Node(new Symbol("print"),
            "foo",
            new Node(new Symbol("add"), 1, 1),
            new Node(new Symbol("bar"))
        );

        Node n2 = new Node(new Symbol("print"),
            "foo",
            new Node(new Symbol("add"), 1, 1),
            new Node(new Symbol("bar"))
        );

        Node n3 = new Node(new Symbol("print"),
            "foo",
            new Node(new Symbol("baz"))
        );

        Assert.assertEquals(n1, n2);
        Assert.assertNotEquals(n1, n3);
    }

    @Test
    public void testToString() {
        Node n1 = new Node(new Symbol("print"),
            "foo",
            new Node(new Symbol("bar"))
        );

        Assert.assertEquals("print(\"foo\", bar())", n1.toString());
    }

    @Test
    public void testToPrettyString() {
        Node n1 = new Node(new Symbol("print"),
            "foo",
            new Node(new Symbol("add"), 1, 1),
            new Node(new Symbol("bar"))
        );

        String expected = "print(\n  \"foo\"\n  add(\n    1\n    1\n  )\n  bar()\n)";
        Assert.assertEquals(expected, n1.toPrettyString());
    }

    @Test
    public void testSplitChain() {
        // Get first child is needed because parse wraps everything in a node by default.
        Node chain = (Node)Parser.parse("a.b.c.d").getFirstChild();
        Node output = Node.splitAccessChain(chain, new Symbol("."));

        Assert.assertEquals(output.getFirstChild(), new Node(null, new Symbol("a"), new Symbol("b"), new Symbol("c"), new Symbol("d")));
    }

    @Test
    public void testReplaceSymbol() {
        // Get first child is needed because parse wraps everything in a node by default.
        Node input = (Node)Parser.parse("a.b.ZZZ.d + ZZZ.foo - ZZZ(4, ZZZ(ZZZ, 5 + ZZZ))").getFirstChild();
        Node expected = (Node)Parser.parse("a.b.AAA.d + AAA.foo - AAA(4, AAA(AAA, 5 + AAA))").getFirstChild();

        Node output = Node.replaceSymbol(input, new Symbol("ZZZ"), new Symbol("AAA"));

        Assert.assertEquals(expected, output);
    }

    @Test
    public void testMetaData() {
        // Get first child is needed because parse wraps everything in a node by default.
        Node node = Node.withMeta(new PersistentMap("line", 1), new Symbol("baz"), 1, 2, 3);

        Node expected = (Node)Parser.parse("baz(1, 2, 3)").getFirstChild();
        Assert.assertEquals(expected, node);
        Assert.assertEquals(1, node.getMeta().map.valAt("line"));
    }
}





