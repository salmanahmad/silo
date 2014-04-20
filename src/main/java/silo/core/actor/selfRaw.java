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

@Function.Definition
public class selfRaw extends Function {

    @Function.Body
    public static silo.lang.Actor invoke(ExecutionContext context) {
        return context.fiber.actor;
    }
}