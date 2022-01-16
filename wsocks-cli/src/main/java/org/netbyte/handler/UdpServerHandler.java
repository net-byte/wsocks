package org.netbyte.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.netbyte.model.Config;
import org.netbyte.model.ProxyAddr;
import org.netbyte.model.UdpPacket;
import org.netbyte.utils.SocksServerUtils;

import java.net.URI;
import java.util.Objects;

/**
 * The local udp sever handler
 */
public final class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final Config config;

    public UdpServerHandler(Config config) {
        this.config = config;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        UdpPacket udpPacket = SocksServerUtils.parseUdpPacket(msg);
        if (Objects.isNull(udpPacket)) {
            ctx.fireChannelRead(msg);
            return;
        }
        final Channel inboundChannel = ctx.channel();
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                (FutureListener<Channel>) future -> {
                    if (!future.isSuccess()) {
                        SocksServerUtils.closeOnFlush(ctx.channel());
                    }
                });
        URI uri = new URI(config.getScheme() + "://" + config.getHost() + ":" + config.getPort() + config.getPath());
        String scheme = config.getScheme();
        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
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
        proxyAddr.setHost(udpPacket.getToHost());
        proxyAddr.setPort(String.valueOf(udpPacket.getToPort()));
        proxyAddr.setNetwork("udp");
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
                                    new WebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()), promise, inboundChannel, proxyAddr, config.getCipherKey(), config.getObfs(), udpPacket));
                        }
                    });
            b.connect(uri.getHost(), uri.getPort()).addListener(future -> {
                if (!future.isSuccess()) {
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        SocksServerUtils.closeOnFlush(ctx.channel());
    }
}
