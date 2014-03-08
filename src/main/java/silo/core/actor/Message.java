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

package silo.core.actor;

public class Message {
    public String id;
    public Object payload;
    public Throwable error;

    public Message(String id, Object payload, Throwable error) {
        this.id = id;
        this.payload = payload;
        this.error = error;
    }

}