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

@Function.Definition
public class yield extends Function {

    @Function.Body
    public static Object invoke(ExecutionContext context) {
        switch(context.programCounter) {
            case -1:
                ExecutionFrame frame = new ExecutionFrame();
                frame.programCounter = 0;

                context.setCurrentFrame(frame);
                context.yielding = true;
                break;
            default:
                context.setCurrentFrame(null);
                context.yielding = false;
                break;
        }

        return null;
    }
}