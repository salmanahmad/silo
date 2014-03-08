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

package silo.net.http.connection;

import com.github.krukow.clj_lang.IPersistentMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;

public class Connection {

    public static int SERVER = 0;
    public static int CLIENT = 1;

    public int type;

    public Channel context;
    public boolean doneReading = false;
    public boolean doneWriting = false;
    public boolean closed = false;

    // The id of the message. Note that chunks are ONLY delivered to
    // the actor that the server spawns. If you want other actors to
    // recieve the message you need to forward the message. Notable,
    // spawning a new actor, passing in the connection and then
    // "reading" the connection will *NOT* work becuase the message
    // will not be sent to that actor.
    public String actorId;
    public String connectionId;

    // TODO: Enable pattern matching
    // TODO: A FuturePattern is a pattern that has a function
    // associated with it that executes the "continuation". This
    // allows you to easily parallelize an Actor code while not
    // spawning a new actor and while leveraging the Actor's mailbox.
    // public Pattern channel;
    // public Pattern futurePattern;
    // public Pattern recievablePattern;
    // public Pattern messagePattern;

    public boolean isKeepAlive = false;
}