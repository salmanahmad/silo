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

import silo.lang.Runtime;
import silo.lang.Function;
import silo.lang.PersistentMapHelper;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import com.github.krukow.clj_lang.IPersistentMap;

public class HttpServer implements Runnable {

    public Runtime runtime;
    public Function handler;
    public IPersistentMap options;

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        Integer port = (Integer)PersistentMapHelper.get(options, "port");

        try {
            HttpServerInitializer initializer = new HttpServerInitializer();
            initializer.runtime = runtime;
            initializer.handler = handler;
            initializer.options = options;

            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(initializer);

            Channel ch = b.bind(port.intValue()).sync().channel();
            ch.closeFuture().sync();
            //ch.closeFuture().await();
        } catch(InterruptedException exception) {
            // TODO: Any clean up tasks needed?
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}