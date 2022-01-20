package org.netbyte.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.netbyte.model.Config;
import org.netbyte.model.ProxyAddr;
import org.netbyte.utils.Cipher;
import org.netbyte.utils.SocksServerUtils;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * WebSocketProxyHandler
 */
public class WebSocketProxyHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    /**
     * The config
     */
    private Config config;
    /**
     * Proxy address
     */
    private ProxyAddr proxyAddr;
    /**
     * Outbound channel
     */
    private Channel outboundChannel;

    public WebSocketProxyHandler(Config config, Channel outboundChannel, ProxyAddr proxyAddr) {
        this.config = config;
        this.outboundChannel = outboundChannel;
        this.proxyAddr = proxyAddr;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (!(frame instanceof BinaryWebSocketFrame)) {
            SocksServerUtils.closeOnFlush(ctx.channel());
            return;
        }
        if (!outboundChannel.isActive()) {
            SocksServerUtils.closeOnFlush(ctx.channel());
            return;
        }
        ByteBuf byteBuf = frame.content();
        ByteBuf writeByteBuf;
        if (Boolean.TRUE.equals(config.getObfs())) {
            byte[] xorData = Cipher.xor(ByteBufUtil.getBytes(byteBuf), config.getCipherKey());
            writeByteBuf = Unpooled.copiedBuffer(xorData);
        } else {
            writeByteBuf = byteBuf;
        }
        if (Objects.equals(proxyAddr.getNetwork(), "udp")) {
            outboundChannel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(writeByteBuf), new InetSocketAddress(proxyAddr.getHost(), Integer.parseInt(proxyAddr.getPort())))).addListener(future -> {
                if (!future.isSuccess()) {
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            });
        } else {
            outboundChannel.writeAndFlush(Unpooled.copiedBuffer(writeByteBuf)).addListener(future -> {
                if (!future.isSuccess()) {
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            });
        }
    }
}
