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

package silo.cli;

import silo.lang.Function;
import silo.lang.ExecutionContext;
import silo.lang.Runtime;

@Function.Definition
public class getLocal extends Function {

    @Function.Body
    public static Object invoke(ExecutionContext context, Object local) {
        Runtime runtime = context.fiber.actor.runtime;
        return runtime.registry.get("silo.cli.shell.locals." + local.toString());
    }
}