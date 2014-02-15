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

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

@Function.Definition(varargs = true)
public class yield extends Function {

    @Function.Body
    public static Object invoke(ExecutionContext context, IPersistentVector args) {
        Fiber fiber = context.fiber;

        switch(context.programCounter) {
            case -1:
                // TODO: What if I want to yield and let another actor execute rather than return control back completely?
                // Perhaps that should be in actor.yield() as opposed to fiber.yield()?

                if(fiber.actor != null && fiber.actor.fiber == fiber) {
                    // Only acknowledgeAttempts and yield if the fiber is the actor's main fiber
                    // otherwise when I fiber yields it will not yield back control to the calling
                    // fiber but pause all execution, which is not what we want.
                    fiber.actor.acknowledgeAttempts();
                }

                if(fiber != null) {
                    fiber.value = args.nth(0, null);
                }

                ExecutionFrame frame = new ExecutionFrame();
                frame.programCounter = 0;

                context.setCurrentFrame(frame);
                context.yielding = true;
                return null;
            default:
                context.setCurrentFrame(null);
                context.yielding = false;

                if(fiber != null) {
                    return fiber.resumedArgument;
                } else {
                    return null;
                }
        }
    }
}