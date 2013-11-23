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

import silo.util.Helper;
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
                Parser parser = new Parser();
                String source = Helper.readResource(path);
                Node program = parser.parse(source);
                //System.out.println(program.toPrettyString());
            } catch(Exception e) {
                System.out.println("Failed on test: " + path);
                throw e;
            }
        }
    }

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
