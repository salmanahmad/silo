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

import silo.lang.PersistentMap;

import io.netty.buffer.ByteBuf;

public class HttpContentMessage {

    public String connectionId;
    public ByteBuf content;

    public boolean lastHttpMessage;
    public PersistentMap trailers;

    public static HttpContentMessage normalContentMessage(String connectionId, ByteBuf content) {
        HttpContentMessage message = new HttpContentMessage();
        message.connectionId = connectionId;
        message.content = content;

        return message;
    }

    public static HttpContentMessage lastContentMessage(String connectionId, ByteBuf content, PersistentMap headersMap) {
        HttpContentMessage message = new HttpContentMessage();
        message.connectionId = connectionId;
        message.content = content;
        message.lastHttpMessage = true;
        message.trailers = headersMap;

        return message;
    }
}