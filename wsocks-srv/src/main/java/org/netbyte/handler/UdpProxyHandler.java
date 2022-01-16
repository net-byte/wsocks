package org.netbyte.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import org.netbyte.model.Config;
import org.netbyte.model.ProxyAddr;
import org.netbyte.utils.Cipher;
import org.netbyte.utils.SocksServerUtils;

import java.net.InetSocketAddress;

/**
 * UDP proxy handler
 */
public class UdpProxyHandler extends ChannelInboundHandlerAdapter {
    private final Promise<Channel> promise;
    private final Channel inboundChannel;
    private final Config config;
    private byte[] firstData;
    private ProxyAddr proxyAddr;

    public UdpProxyHandler(Promise<Channel> promise, Channel inboundChannel, Config config, byte[] firstData, ProxyAddr proxyAddr) {
        this.promise = promise;
        this.inboundChannel = inboundChannel;
        this.config = config;
        this.firstData = firstData;
        this.proxyAddr = proxyAddr;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (Boolean.TRUE.equals(config.getObfs())) {
            firstData = Cipher.xor(firstData, config.getCipherKey());
        }
        ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(firstData), new InetSocketAddress(proxyAddr.getHost(), Integer.parseInt(proxyAddr.getPort()))));
        promise.setSuccess(ctx.channel());
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        DatagramPacket packet = (DatagramPacket) msg;
        ByteBuf byteBuf = packet.content();
        if (Boolean.TRUE.equals(config.getObfs())) {
            byte[] xorData = Cipher.xor(ByteBufUtil.getBytes(byteBuf), config.getCipherKey());
            inboundChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(xorData)));
            ReferenceCountUtil.release(byteBuf);
        } else {
            inboundChannel.writeAndFlush(new BinaryWebSocketFrame(byteBuf));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SocksServerUtils.closeOnFlush(inboundChannel);
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        SocksServerUtils.closeOnFlush(ctx.channel());
        promise.setFailure(cause);
    }
}