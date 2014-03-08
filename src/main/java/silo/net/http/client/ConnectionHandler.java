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

package silo.net.http.client;

import silo.lang.Actor;
import silo.core.actor.Message;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelFuture;

class ConnectionHandler implements ChannelFutureListener {
    String id;
    Actor actor;

    public ConnectionHandler(String id, Actor actor) {
        this.id = id;
        this.actor = actor;
    }

    public void operationComplete(ChannelFuture future) {
        if(future.isSuccess()) {
            Message message = new Message(this.id, future.channel(), null);
            actor.inboxPut(message);
        } else {
            Message message = new Message(this.id, null, future.cause());
            actor.inboxPut(message);
        }
    }
}