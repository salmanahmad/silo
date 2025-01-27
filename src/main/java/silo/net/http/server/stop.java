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

import silo.lang.Function;
import silo.lang.ExecutionContext;
import silo.lang.Runtime;

@Function.Definition
public class stop extends Function {

    @Function.Body
    public static Object invoke(ExecutionContext context, Server server) {
        if(server.future != null) {
            server.future.cancel(true);
        }

        return null;
    }
}