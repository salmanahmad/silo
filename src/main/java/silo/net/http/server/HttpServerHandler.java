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

package silo.net.http.server;

import silo.net.http.connection.Connection;
import silo.net.http.connection.Request;
import silo.net.http.connection.HttpContentMessage;

import silo.lang.Actor;
import silo.lang.Runtime;
import silo.lang.Function;
import silo.lang.PersistentMapHelper;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.github.krukow.clj_lang.IPersistentMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
//import static io.netty.handler.codec.http.HttpHeaders.Values.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class HttpServerHandler extends SimpleChannelInboundHandler {

    public static Function handle = null;
    static {
        try {
            handle = (Function)Class.forName("silo.net.http.connection.handle").newInstance();
        } catch(Exception e) {
            throw new RuntimeException("Could not load connection handler.");
        }
    }

    public Runtime runtime;
    public Function handler;
    public IPersistentMap options;

    private Connection connection;
    private Actor actor;

    private HttpRequest request;
    private final StringBuilder buf = new StringBuilder();

    public HttpServerHandler(Runtime runtime, Function handler, IPersistentMap options) {
        this.runtime = runtime;
        this.handler = handler;
        this.options = options;
    }

    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        messageReceived(ctx, msg);
    }

    protected void messageReceived(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest)msg;

            IPersistentMap headersMap = PersistentMapHelper.create();
            HttpHeaders headers = request.headers();
            for (Map.Entry<String, String> h : headers) {
                // TODO: Support multiple duplicate headers
                String key = h.getKey();
                String value = h.getValue();
                headersMap = PersistentMapHelper.set(headersMap, key, value);
            }

            this.connection = new Connection();
            this.connection.context = ctx;
            this.connection.actorId = UUID.randomUUID().toString();
            this.connection.connectionId = UUID.randomUUID().toString();
            this.connection.isKeepAlive = HttpHeaders.isKeepAlive(request);

            Request httpRequest = new Request();
            httpRequest.httpVersion = request.getProtocolVersion().toString();
            httpRequest.method = request.getMethod().toString();
            httpRequest.uri = request.getUri();
            httpRequest.headers = headersMap;

            if (HttpHeaders.is100ContinueExpected(request)) {
                httpRequest.is100ContinueExpected = true;

                if(Boolean.TRUE.equals(PersistentMapHelper.get(this.options, "send-100-continue", Boolean.TRUE))) {
                    send100Continue(ctx);
                }
            } else {
                httpRequest.is100ContinueExpected = false;
            }

            this.actor = runtime.spawn(connection.actorId, handle, handler, connection, httpRequest);

            // TODO: Do I need to check this?
            //appendDecoderResult(buf, request);
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

                message = HttpContentMessage.lastContentMessage(connection.connectionId, content, headersMap);

                //DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer("Hello", CharsetUtil.UTF_8));
                //DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
                //response.headers().set(TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
                //response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                //response.headers().set(CONTENT_LENGTH, 10);
                //response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                //response.headers().set(CONNECTION, HttpHeaders.Values.CLOSE);
                //ctx.write(response);

                //ctx.write(new DefaultHttpContent(Unpooled.copiedBuffer("Hello", CharsetUtil.UTF_8)));
                //ctx.write(new DefaultHttpContent(Unpooled.copiedBuffer("World", CharsetUtil.UTF_8)));
                //ctx.write(LastHttpContent.EMPTY_LAST_CONTENT);

                //ctx.flush();
                //ctx.close();
            } else {
                message = HttpContentMessage.normalContentMessage(connection.connectionId, content);
            }

            this.actor.inboxPut(message);

            // TODO: How do I know when to close the connection?
            //if(!writeResponse(trailer, ctx)) {
            //    //ctx.flush();
            //    //ctx.close();
            //    //ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            //}
        }
    }

    private static void send400BadRequest(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
        ctx.write(response);
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(response);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }



    // TODO: Do I need to check this?
    private static void appendDecoderResult(StringBuilder buf, HttpObject o) {
        DecoderResult result = o.getDecoderResult();
        if (result.isSuccess()) {
            return;
        }

        buf.append(".. WITH DECODER FAILURE: ");
        buf.append(result.cause());
        buf.append("\r\n");
    }

    // TODO: Do I need to check this?
    private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        // Decide whether to close the connection or not.
        boolean keepAlive = isKeepAlive(request);
        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, currentObj.getDecoderResult().isSuccess()? OK : BAD_REQUEST,
                Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Encode the cookie.
        String cookieString = request.headers().get(COOKIE);
        if (cookieString != null) {
            Set<Cookie> cookies = CookieDecoder.decode(cookieString);
            if (!cookies.isEmpty()) {
                // Reset the cookies if necessary.
                for (Cookie cookie: cookies) {
                    response.headers().add(SET_COOKIE, ServerCookieEncoder.encode(cookie));
                }
            }
        } else {
            // Browser sent no cookie.  Add some.
            response.headers().add(SET_COOKIE, ServerCookieEncoder.encode("key1", "value1"));
            response.headers().add(SET_COOKIE, ServerCookieEncoder.encode("key2", "value2"));
        }

        // Write the response.
        ctx.write(response);

        return keepAlive;
    }
}
