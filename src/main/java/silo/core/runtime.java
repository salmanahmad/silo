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

package silo.core;

import silo.lang.ExecutionContext;
import silo.lang.Function;
import silo.lang.Actor;
import silo.lang.Runtime;

@Function.Definition
public class runtime extends Function {

    @Function.Body
    public static Runtime invoke(ExecutionContext context) {
        try {
            return context.fiber.actor.runtime;
        } catch(NullPointerException e) {
            throw new RuntimeException("No runtime available.");
        }
    }
}