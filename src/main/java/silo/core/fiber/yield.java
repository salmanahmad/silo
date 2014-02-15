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
        Fiber fiber = context.currentActor.fiber;

        switch(context.programCounter) {
            case -1:
                // TODO: Enable this...
                //actor.acknowledgeAttempts()

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