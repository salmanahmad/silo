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

package silo.core.actor;

import silo.lang.ExecutionContext;
import silo.lang.Function;
import silo.lang.Actor;

// TODO: alternative names - recv, take, accept

@Function.Definition
public class read extends Function {

    @Function.Body
    public static Object invoke(ExecutionContext context) {
        Actor actor = context.fiber.actor;
        Object o = actor.inboxGet(context);

        return o;
    }
}