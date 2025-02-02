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

public class Request {
    public String connectionId;

    public String httpVersion;
    public String method;
    public String uri;
    public IPersistentMap headers;
    public boolean is100ContinueExpected;
}