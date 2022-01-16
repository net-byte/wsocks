package org.netbyte.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.netbyte.model.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The local socks5 server
 */
@Component
public class SocksServer {
    private final static EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final static EventLoopGroup workerGroup = new NioEventLoopGroup();

    @Value("${local.host}")
    private String localhost;
    @Value("${local.port}")
    private Integer localPort;
    @Value("${ws.host}")
    private String host;
    @Value("${ws.port}")
    private Integer port;
    @Value("${ws.key}")
    private String key;
    @Value("${ws.obfs}")
    private Boolean obfs;
    @Value("${ws.path}")
    private String path;
    @Value("${ws.scheme}")
    private String scheme;

    public void start() {
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.AUTO_READ, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new SocksServerInitializer(new Config(localhost, localPort, host, port, key, obfs, path, scheme)));
            b.bind(localhost, localPort).sync().channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
