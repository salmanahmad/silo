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

import silo.lang.Actor;
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
        Fiber existingFiber = context.fiber;
        Actor existingActor = fiber.actor;

        Object output = null;

        try {
            existingActor.fiber = fiber;
            fiber.actor = existingActor;

            fiber.resumedArgument = vector.nth(0, null);
            output = fiber.function.apply(fiber.context, fiber.arguments);
        } finally {
            if(!fiber.context.yielding) {
                fiber.value = o;
            } else {
                // TODO: If the current fiber is yielding, that means that existingFiber should
                // yield as well. However, I need to figure out how to make sure that I update
                // this method so that I properly handles resumption. Also, I probably need to
                // check that "existingFiber != fiber"
            }

            existingActor.fiber = existingFiber;
        }

        // TODO: Make fibers immutable...
        return fiber;
    }
}