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

import java.util.concurrent.Future;

@Function.Definition
public class Server {
    public HttpServer server;
    public Future future;
}