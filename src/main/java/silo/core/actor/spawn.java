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

import silo.lang.Actor;
import silo.lang.Runtime;
import silo.lang.ExecutionContext;
import silo.lang.Function;

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.RT;

// TODO: This function should accept a vector of arguments intead of being marked as varargs

@Function.Definition(varargs = true)
public class spawn extends Function {

    @Function.Body
    public static String invoke(ExecutionContext context, Function function, IPersistentVector vector) {
        Runtime runtime = context.fiber.actor.runtime;
        Actor actor = runtime.spawn(function, RT.toArray(vector));

        return actor.address;
    }
}