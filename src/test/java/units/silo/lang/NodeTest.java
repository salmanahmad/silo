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
}





