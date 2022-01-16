package org.netbyte.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.netbyte.model.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class WebSocketServer {
    private static final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();

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
        final SslContext sslCtx;
        try {
            if (Objects.equals(scheme, "wss")) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } else {
                sslCtx = null;
            }
            Config config = new Config(key, obfs, path);
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new WebSocketServerInitializer(config, sslCtx));

            Channel ch = b.bind(host, port).sync().channel();
            ch.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
