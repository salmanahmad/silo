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

package silo.net.http.server;

import silo.lang.PersistentMap;
import silo.lang.Function;
import silo.lang.ExecutionContext;
import silo.lang.Runtime;

@Function.Definition
public class build extends Function {

    @Function.Body
    public static Server invoke(ExecutionContext context, Function handler, PersistentMap options) {
        Runtime runtime = context.fiber.actor.runtime;

        HttpServer httpServer = new HttpServer();
        httpServer.runtime = runtime;
        httpServer.handler = handler;
        httpServer.options = options;

        Server server = new Server();
        server.server = httpServer;

        return server;
    }
}