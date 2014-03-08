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
import silo.lang.PersistentMapHelper;
import silo.net.http.connection.Response;
import silo.net.http.connection.HttpContentMessage;

import java.util.Map;
import com.github.krukow.clj_lang.IPersistentMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class HttpClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    String connectionId;
    Actor actor;

    public HttpClientHandler(String connectionId, Actor actor) {
        this.connectionId = connectionId;
        this.actor = actor;
    }

    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        messageReceived(ctx, msg);
    }

    public void messageReceived(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            IPersistentMap headersMap = PersistentMapHelper.create();
            HttpHeaders headers = response.headers();
            for (Map.Entry<String, String> h : headers) {
                // TODO: Support multiple duplicate headers
                String key = h.getKey();
                String value = h.getValue();
                headersMap = PersistentMapHelper.set(headersMap, key, value);
            }

            Response message = new Response();
            message.connectionId = connectionId;
            message.status = response.getStatus().code();
            message.headers = headersMap;

            actor.inboxPut(message);
        }
        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent)msg;

            ByteBuf content;
            if(httpContent.content().isReadable()) {
                content = httpContent.content().copy();
            } else {
                content = Unpooled.buffer(0);
            }

            HttpContentMessage message = null;

            if (msg instanceof LastHttpContent) {
                LastHttpContent trailer = (LastHttpContent)msg;

                IPersistentMap headersMap = null;
                HttpHeaders headers = trailer.trailingHeaders();
                if(!headers.isEmpty()) {
                    headersMap = PersistentMapHelper.create();

                    for (Map.Entry<String, String> h : headers) {
                        // TODO: Support multiple duplicate headers
                        String key = h.getKey();
                        String value = h.getValue();
                        headersMap = PersistentMapHelper.set(headersMap, key, value);
                    }
                }

                message = HttpContentMessage.lastContentMessage(connectionId, content, headersMap);
            } else {
                message = HttpContentMessage.normalContentMessage(connectionId, content);
            }

            this.actor.inboxPut(message);
        }
    }

    @Override
    public void exceptionCaught(
            ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
