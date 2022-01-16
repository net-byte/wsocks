package org.netbyte.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.netbyte.model.Config;
import org.netbyte.model.ProxyAddr;
import org.netbyte.utils.SocksServerUtils;

import java.net.URI;

/**
 * The socks5 command handler
 */
public final class ConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private final Config config;

    public ConnectHandler(Config config) {
        this.config = config;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) throws Exception {
        if (!(message instanceof Socks5CommandRequest)) {
            ctx.fireChannelRead(message);
            return;
        }
        final Socks5CommandRequest request = (Socks5CommandRequest) message;
        final Channel inboundChannel = ctx.channel();
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                (FutureListener<Channel>) future -> {
                    final Channel websocketChannel = future.getNow();
                    if (future.isSuccess()) {
                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                Socks5CommandStatus.SUCCESS,
                                request.dstAddrType(),
                                request.dstAddr(),
                                request.dstPort())).addListener((ChannelFutureListener) f -> {
                            if (f.isSuccess()) {
                                ctx.pipeline().addLast(new ToServerTcpHandler(websocketChannel, config.getCipherKey(), config.getObfs()));
                                ctx.pipeline().remove(this);
                            } else {
                                SocksServerUtils.closeOnFlush(ctx.channel());
                            }
                        });
                    } else {
                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                        SocksServerUtils.closeOnFlush(ctx.channel());
                    }
                });
        URI uri = new URI(config.getScheme() + "://" + config.getHost() + ":" + config.getPort() + config.getPath());
        String scheme = config.getScheme();
        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
            SocksServerUtils.closeOnFlush(ctx.channel());
            return;
        }
        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }
        ProxyAddr proxyAddr = new ProxyAddr();
        proxyAddr.setHost(request.dstAddr());
        proxyAddr.setPort(String.valueOf(request.dstPort()));
        proxyAddr.setNetwork("tcp");
        proxyAddr.setKey(config.getKey());
        try {
            Bootstrap b = new Bootstrap();
            b.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.AUTO_READ, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), config.getHost(), config.getPort()));
                            }
                            p.addLast(
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(8192),
                                    new WebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()), promise, inboundChannel, proxyAddr, config.getCipherKey(), config.getObfs()));
                        }
                    });
            b.connect(uri.getHost(), uri.getPort()).addListener(future -> {
                if (!future.isSuccess()) {
                    ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}