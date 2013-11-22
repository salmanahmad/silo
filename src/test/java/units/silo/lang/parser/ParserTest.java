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

import silo.lang.compiler.*;
import silo.lang.compiler.grammar.*;

public class ParserTest {

    @Test
    public void testEmpty() {
        Parser parser = new Parser();
        parser.parse("");
        parser.parse("\n\n\n");
        parser.parse("\n\n  \n \n");
    }

    @Test
    public void testSymbol() {
        Parser parser = new Parser();
        parser.parse("foo");
    }

    @Test
    public void testChildlessNode() {
        Parser parser = new Parser();
        parser.parse("foo()");
    }

    @Test
    public void testOneChildNode() {
        Parser parser = new Parser();
        parser.parse("foo(bar)");
    }

    @Test
    public void testManyChildrenNode() {
        Parser parser = new Parser();
        parser.parse("foo(bar, baz, qux)");
    }

    @Test
    public void testNestedChildren() {
        Parser parser = new Parser();
        parser.parse("foo(bar(), baz, qux())");
    }

}
