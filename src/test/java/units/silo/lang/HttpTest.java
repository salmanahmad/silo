/*
 *
 *  Copyright 2014 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */


import silo.lang.*;
import silo.lang.Runtime;
import silo.lang.compiler.Parser;

import org.junit.*;
import java.util.*;

public class HttpTest {

    @Test
    public void testServerSimple() throws Exception {
        Runtime runtime = new Runtime(new RuntimeClassLoader(), 1);
        String source = Helper.readResource("/http-test/simple.silo");
        Vector<Class> classes = runtime.compile(Parser.parse(source));

        Actor server = runtime.spawn("simpleServer");
        Thread.sleep(1000);
        server.inboxPut("Stop");
        server.await();
    }
}
