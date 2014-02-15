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

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

@Function.Definition(varargs = true)
public class resume extends Function {

    @Function.Body
    public static Object invoke(ExecutionContext context, Fiber fiber, IPersistentVector vector) {
        // TODO: Invoke should look at the list of arguments stored in Fiber
        // and dispatch "apply" on that number in a performant manner.

        // TODO: Handle exception handling here.
        Fiber existingFiber = context.currentActor.fiber;

        try {
            context.currentActor.fiber = fiber;

            fiber.resumedArgument = vector.nth(0, null);
            Object o = fiber.function.apply(fiber.context, fiber.arguments);

            if(!fiber.context.yielding) {
                fiber.value = o;
            }
        } finally {
            context.currentActor.fiber = existingFiber;
        }

        // TODO: Make fibers immutable...
        return fiber;
    }
}