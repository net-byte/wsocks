package org.netbyte.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import org.netbyte.handler.SocksServerHandler;
import org.netbyte.model.Config;

public class SocksServerInitializer extends ChannelInitializer<SocketChannel> {
    final private Config config;

    public SocksServerInitializer(Config config) {
        this.config = config;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(
                // new LoggingHandler(LogLevel.INFO),
                new SocksPortUnificationServerHandler(),
                new SocksServerHandler(config));
    }
}