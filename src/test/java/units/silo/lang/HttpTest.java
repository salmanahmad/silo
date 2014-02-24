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


import org.junit.*;
import java.util.*;

public class HttpTest {

    @Test
    public void testServer() throws Exception {
        silo.lang.PersistentMap options = new silo.lang.PersistentMap();
        options = options.set("port", new Integer(8080));

        silo.net.http.HttpServer server = new silo.net.http.HttpServer();
        server.options = options;

        server.run();
    }
}
