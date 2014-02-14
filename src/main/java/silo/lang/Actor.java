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

public class Actor {
    String address;
    ExecutionContext context;

    ArrayDeque inbox = new ArrayDeque();
    ArrayDeque drain = new ArrayDeque();

    public Actor(String address, ExecutionContext context) {
        this.address = address;
        this.context = context;
    }

    public String address() {
        return address;
    }

    public synchronized boolean inboxEmpty() {
        return inbox.size() == 0;
    }

    public synchronized Object inboxPeek() {
        if(inbox.size() == 0) {
            // TODO: Block the execution context and go to sleep
            return null;
        } else {
            return inbox.getLast();
        }
    }

    public synchronized Object inboxSkip() {
        if(inbox.size() == 0) {
            // TODO: Block the execution context and go to sleep
            return null;
        } else {
            Object o = inbox.removeLast();
            drain.addLast(o);

            return o;
        }
    }

    public synchronized Object inboxGet() {
        if(inbox.size() == 0) {
            // TODO: Block the execution context and go to sleep
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
        return value;
    }
}
