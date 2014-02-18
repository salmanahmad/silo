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

package silo.lang;

import java.util.ArrayDeque;

// TODO: move Fiber into silo.lang
import silo.core.fiber.Fiber;

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

public class Actor implements Runnable {
    public Runtime runtime;
    public String address;
    public Fiber fiber;

    ArrayDeque inbox = new ArrayDeque();
    ArrayDeque drain = new ArrayDeque();

    boolean done = false;
    boolean running = false;
    boolean yielding = false;

    int scheduleAttempts = 0;
    int acknowledgedAttempt = 0;

    public Actor(Runtime runtime, String address, Fiber fiber) {
        this.runtime = runtime;
        this.address = address;
        this.fiber = fiber;

        this.fiber.actor = this;
    }

    public synchronized boolean inboxEmpty() {
        return inbox.size() == 0;
    }

    private void block(ExecutionContext context) {
        ExecutionFrame frame = new ExecutionFrame();
        frame.programCounter = 0;

        context.setCurrentFrame(frame);
        context.yielding = true;
    }

    public synchronized Object inboxPeek(ExecutionContext context) {
        if(inbox.size() == 0) {
            acknowledgeAttempts();
            block(context);
            return null;
        } else {
            return inbox.getLast();
        }
    }

    public synchronized Object inboxSkip(ExecutionContext context) {
        if(inbox.size() == 0) {
            acknowledgeAttempts();
            block(context);
            return null;
        } else {
            Object o = inbox.removeLast();
            drain.addLast(o);

            return o;
        }
    }

    public synchronized Object inboxGet(ExecutionContext context) {
        if(inbox.size() == 0) {
            acknowledgeAttempts();
            block(context);
            return null;
        } else {
            Object o = inbox.removeLast();

            int size = drain.size();
            for(int i = 0; i < size; i++) {
                inbox.addLast(drain.removeFirst());
            }

            return o;
        }
    }

    public synchronized Object inboxPut(Object value) {
        inbox.addFirst(value);
        schedule();
        return value;
    }

    public synchronized void acknowledgeAttempts() {
        acknowledgedAttempt = scheduleAttempts;
    }

    public synchronized void yield() {
        this.running = false;
        this.yielding = true;
    }

    public synchronized void schedule() {
        if(done) {
            return;
        }

        if(running) {
            scheduleAttempts++;
        } else {
            this.running = true;
            this.yielding = false;
            this.runtime.actorExecutor.submit(this);
        }
    }

    public synchronized boolean shouldRun() {
        if(done) {
            return false;
        }

        if(yielding) {
            return false;
        }

        if(acknowledgedAttempt == scheduleAttempts) {
            running = false;
            acknowledgedAttempt = 0;
            scheduleAttempts = 0;

            return false;
        } else {
            return true;
        }
    }

    public void run() {
        boolean firstTime = true;

        while(firstTime || shouldRun()) {
            firstTime = false;

            silo.core.fiber.resume.invoke(fiber.context, fiber, PersistentVector.emptyVector());

            if(!fiber.context.yielding) {
                synchronized(this) {
                    done = true;
                    runtime.actors.remove(address);
                    this.notifyAll();
                }
            }
        }

        synchronized(this) {
            if(this.yielding) {
                schedule();
            }
        }
    }

    public Object await() {
        synchronized(this) {
            while(!done) {
                try {
                    this.wait();
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return fiber.value;
    }
}
