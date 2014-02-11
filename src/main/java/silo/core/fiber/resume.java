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

package silo.core.fiber;

import silo.lang.ExecutionContext;
import silo.lang.ExecutionFrame;
import silo.lang.Function;

import java.util.Arrays;

@Function.Definition
public class resume extends Function {

    @Function.Body
    public static Object invoke(ExecutionContext context, Fiber fiber) {
        // TODO: Invoke should look at the list of arguments stored in Fiber
        // and dispatch "apply" on that number in a performant manner.

        fiber.function.apply(fiber.context);
        return null;
    }
}