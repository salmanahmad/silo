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

import silo.lang.Actor;
import silo.lang.Runtime;
import silo.lang.Function;
import silo.lang.ExecutionContext;
import silo.lang.ExecutionFrame;

import silo.core.actor.Message;

import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;

@Function.Definition
public class sleep extends Function {

    public static class Task extends TimerTask {
        public String id = null;
        public Actor actor = null;

        public Task(String id, Actor actor) {
            this.id = id;
            this.actor = actor;
        }

        public void run() {
            actor.inboxPut(new Message(id, null, null));
        }
    }

    @Function.Body
    public static Object invoke(ExecutionContext context, long ms) {
        Actor actor = context.fiber.actor;
        Runtime runtime = actor.runtime;

        String registryKey = "silo.core.timer";
        String id = null;

        if(context.programCounter == -1) {
            Timer t = null;
            Object o = null;

            id = UUID.randomUUID().toString();

            o = runtime.registry.get(registryKey);
            if(o == null) {
                synchronized(runtime.registry) {
                    // Check again in case someone else set the key from another thread
                    o = runtime.registry.get(registryKey);
                    if(o == null) {
                        o = new Timer(true);
                        runtime.registry.put(registryKey, o);
                    }
                }
            }

            t = (Timer)o;
            t.schedule(new Task(id, actor), ms);
        } else {
            ExecutionFrame frame = context.getCurrentFrame();
            id = (String)frame.locals[0];
        }

        while(true) {
            Object o = actor.inboxPeek(context);
            if(context.yielding) {
                ExecutionFrame frame = new ExecutionFrame();
                frame.programCounter = 0;
                frame.locals = new Object[] { id };

                context.setCurrentFrame(frame);
                return null;
            } else {
                if(o instanceof Message) {
                    Message message = (Message)o;
                    if(message.id.equals(id)) {
                        actor.inboxGet(context);
                        return null;
                    }
                }

                actor.inboxSkip(context);
            }
        }
    }
}