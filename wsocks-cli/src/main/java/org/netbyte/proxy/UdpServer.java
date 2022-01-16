package org.netbyte.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.netbyte.handler.UdpServerHandler;
import org.netbyte.model.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The local udp server
 */
@Component
public class UdpServer {
    private final static EventLoopGroup bossGroup = new NioEventLoopGroup();

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
            Bootstrap b = new Bootstrap();
            b.group(bossGroup)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.AUTO_CLOSE, true)
                    .option(ChannelOption.AUTO_READ, true)
                    .handler(new UdpServerHandler(new Config(localhost, localPort, host, port, key, obfs, path, scheme)));
            b.bind(localhost, localPort).sync().channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
        }
    }
}
