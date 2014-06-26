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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

// TODO: move Fiber into silo.lang
import silo.core.fiber.Fiber;

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

public class Actor implements Runnable {

    private final static Object NULL = new Object();

    public static class Task extends ForkJoinTask {
        public final Actor actor;

        public Task(Actor actor) {
            this.actor = actor;
        }

        public boolean exec() {
            actor.run();
            return true;
        }

        public Object getRawResult() {
            return null;
        }

        public void setRawResult(Object o) {

        }
    }


    public Runtime runtime;
    public String address;
    public Fiber fiber;

    Throwable error = null;

    ArrayDeque inbox = new ArrayDeque();
    ArrayDeque drain = new ArrayDeque();

    boolean done = false;
    boolean running = false;
    boolean yielding = false;
    boolean locked = false;

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

    private synchronized void blockFiber(ExecutionContext context) {
        acknowledgeAttempts();

        ExecutionFrame frame = new ExecutionFrame();
        frame.programCounter = 0;

        context.setCurrentFrame(frame);
        context.yielding = true;
    }

    private synchronized void blockThread() {
        acknowledgeAttempts();

        while(inbox.size() == 0) {
            try {
                this.wait();
            } catch(InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized Object inboxPeek(ExecutionContext context) {
        if(inbox.size() == 0) {
            if(this.locked) {
                blockThread();
            } else {
                blockFiber(context);
                return null;
            }
        }

        Object o = inbox.getLast();

        if(o == NULL) {
            o = null;
        }

        return o;
    }

    public synchronized Object inboxSkip(ExecutionContext context) {
        if(inbox.size() == 0) {
            if(this.locked) {
                blockThread();
            } else {
                blockFiber(context);
                return null;
            }
        }

        Object o = inbox.removeLast();
        drain.addLast(o);

        if(o == NULL) {
            o = null;
        }

        return o;
    }

    public synchronized Object inboxGet(ExecutionContext context) {
        if(inbox.size() == 0) {
            if(this.locked) {
                blockThread();
            } else {
                blockFiber(context);
                return null;
            }
        }

        Object o = inbox.removeLast();

        int size = drain.size();
        for(int i = 0; i < size; i++) {
            inbox.addLast(drain.removeFirst());
        }

        if(o == NULL) {
            o = null;
        }

        return o;
    }

    public synchronized Object inboxPut(Object value) {
        if(value == null) {
            value = NULL;
        }

        inbox.addFirst(value);
        schedule();
        return value;
    }

    public synchronized void acknowledgeAttempts() {
        acknowledgedAttempt = scheduleAttempts;
    }

    public synchronized void yield() {
        this.yielding = true;
    }

    public synchronized void lock() {
        this.locked = true;
    }

    public synchronized void unlock() {
        this.locked = false;
    }

    public synchronized void schedule(boolean shouldFork) {
        if(done) {
            return;
        }

        if(running) {
            scheduleAttempts++;

            if(this.locked) {
                // If the actor has locked a thread notify it
                // in case it is blocking on the inbox.
                this.notifyAll();
            }
        } else {
            this.running = true;
            this.yielding = false;
            if(this.locked) {
                this.runtime.backgroundExecutor.submit(this);
            } else {
                //this.runtime.actorExecutor.submit(this);
                if(shouldFork) {
                    (new Task(this)).fork();
                } else {
                    ((ForkJoinPool)this.runtime.actorExecutor).submit((new Task(this)));
                }
            }
        }
    }

    public synchronized void schedule() {
        schedule(true);
    }

    private synchronized boolean shouldRun() {
        if(done) {
            return false;
        }

        if(yielding) {
            running = false;
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
        // TODO: To be super safe, should I wrap this in a "runlock"?

        boolean firstTime = true;

        while(firstTime || shouldRun()) {
            firstTime = false;

            try {
                silo.core.fiber.resume.invoke(fiber.context, fiber, PersistentVector.emptyVector());
            } catch(Exception e) {
                System.err.println("Error: actor (" + this.address + ") encountered an uncaught exception.");
                e.printStackTrace(System.err);

                this.error = e;

                awake();
                return;
            }

            if(!fiber.context.yielding) {
                awake();
            }
        }

        synchronized(this) {
            if(this.yielding) {
                schedule();
            }
        }
    }

    private synchronized void awake() {
        done = true;
        runtime.actors.remove(address);
        this.notifyAll();
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

        if(error == null) {
            return fiber.value;
        } else {
            throw new RuntimeException(error);
        }
    }
}
