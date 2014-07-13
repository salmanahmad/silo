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
import silo.lang.Function;
import silo.lang.ExecutionContext;
import silo.lang.ExecutionFrame;
import silo.lang.Runtime;
import silo.lang.PersistentMapHelper;

import silo.core.actor.Message;
import silo.net.http.connection.Connection;

import com.github.krukow.clj_lang.IPersistentMap;

import java.util.UUID;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

// TODO: Implement this method directly in Silo. No need for it to be in Java. I just need to implement
// monitor-enter and monitor-exit and give access to the ExecutionContext.

@Function.Definition
public class connect extends Function {

    public static class EventLoopGroupCleaner implements Runnable {
        public EventLoopGroup group;

        public void run() {
            try {
                synchronized(this) {
                    this.wait();
                }
            } catch(InterruptedException e) {
                // Do Nothing
            } finally {
                group.shutdownGracefully();
            }
        }
    }

    @Function.Body
    public static Connection invoke(ExecutionContext context, String host, int port) throws Throwable {
        Actor actor = context.fiber.actor;
        Runtime runtime = actor.runtime;
        Connection connection = null;

        if(context.programCounter == -1) {
            connection = new Connection();
            connection.type = Connection.CLIENT;
            connection.actorId = actor.address;
            connection.connectionId = UUID.randomUUID().toString();

            Bootstrap b = null;
            Object o = null;

            o = runtime.registry.get("silo.net.http.client.bootstrap");
            if(o == null) {
                synchronized(runtime.registry) {
                    // Check again in case someone else set the bootstrap from another thread
                    o = runtime.registry.get("silo.net.http.client.bootstrap");
                    if(o == null) {
                        EventLoopGroup group = new NioEventLoopGroup();

                        EventLoopGroupCleaner cleaner = new EventLoopGroupCleaner();
                        cleaner.group = group;
                        runtime.backgroundExecutor.submit(cleaner);

                        o = new Bootstrap()
                            .group(group)
                            .channel(NioSocketChannel.class)
                            .handler(new HttpClientInitializer(connection.connectionId, actor));

                        runtime.registry.put("silo.net.http.client.bootstrap", o);
                    }
                }
            }

            b = (Bootstrap)o;
            b.connect(host, port).addListener(new ConnectionHandler(connection.connectionId, actor));
        } else {
            ExecutionFrame frame = context.getCurrentFrame();
            connection = (Connection)((Object[])frame.locals)[0];
        }

        while(true) {
            Object o = actor.inboxPeek(context);
            if(context.yielding) {
                ExecutionFrame frame = new ExecutionFrame();
                frame.programCounter = 0;
                frame.locals = new Object[] { connection };

                context.setCurrentFrame(frame);
                return null;
            } else {
                if(o instanceof Message) {
                    Message message = (Message)o;
                    if(message.id.equals(connection.connectionId)) {
                        if(message.error == null) {
                            actor.inboxGet(context);
                            connection.context = (Channel)message.payload;
                            return connection;
                        } else {
                            throw message.error;
                        }
                    }
                }

                actor.inboxSkip(context);
            }
        }
    }
}
