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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

// TODO: Re-implement this once you implement monitor-enter and monitor-exit

@Function.Definition
public class readln extends Function {

    public static class Task implements Runnable {
        public String id = null;
        public Actor actor = null;

        public Task(String id, Actor actor) {
            this.id = id;
            this.actor = actor;
        }

        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            try {
               String line = reader.readLine();
               actor.inboxPut(new Message(id, line, null));
            } catch (IOException error) {
               actor.inboxPut(new Message(id, null, error));
            }
        }
    }

    @Function.Body
    public static String invoke(ExecutionContext context) throws Throwable {
        Actor actor = context.fiber.actor;
        Runtime runtime = actor.runtime;

        String registryKey = "silo.core.timer";
        String id = null;

        if(context.programCounter == -1) {
            id = UUID.randomUUID().toString();
            runtime.backgroundExecutor.submit(new Task(id, actor));
        } else {
            ExecutionFrame frame = context.getCurrentFrame();
            id = (String)((Object[])frame.locals)[0];
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
                    if(message.error == null) {
                        actor.inboxGet(context);
                        return (String)message.payload;
                    } else {
                        throw message.error;
                    }
                }

                actor.inboxSkip(context);
            }
        }
    }
}