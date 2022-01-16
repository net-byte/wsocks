package org.netbyte.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import org.netbyte.model.ProxyAddr;
import org.netbyte.model.UdpPacket;
import org.netbyte.utils.Cipher;
import org.netbyte.utils.SocksServerUtils;

import java.net.InetSocketAddress;


/**
 * Send data from remote websocket server to local client
 */
public final class ToLocalUdpHandler extends ChannelInboundHandlerAdapter {

    private final Channel inboundChannel;
    private final byte[] cipherKey;
    private final Boolean obfs;
    private UdpPacket udpPacket;

    public ToLocalUdpHandler(Channel inboundChannel, byte[] cipherKey, Boolean obfs, UdpPacket udpPacket) {
        this.inboundChannel = inboundChannel;
        this.cipherKey = cipherKey;
        this.obfs = obfs;
        this.udpPacket = udpPacket;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!inboundChannel.isActive()) {
            ReferenceCountUtil.release(msg);
            return;
        }
        if (msg instanceof FullHttpResponse) {
            ReferenceCountUtil.release(msg);
            return;
        }
        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) frame;
            if (Boolean.TRUE.equals(obfs)) {
                ByteBuf byteBuf = binaryFrame.content();
                byte[] xorData = Cipher.xor(ByteBufUtil.getBytes(byteBuf), cipherKey);
                inboundChannel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(SocksServerUtils.joinByteArray(udpPacket.getHeader(), xorData)), new InetSocketAddress(udpPacket.getFromHost(), udpPacket.getFromPort()))).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        inboundChannel.close();
                    }
                });
                ReferenceCountUtil.release(byteBuf);
            } else {
                byte[] data = ByteBufUtil.getBytes(binaryFrame.content());
                inboundChannel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(SocksServerUtils.joinByteArray(udpPacket.getHeader(), data)), new InetSocketAddress(udpPacket.getFromHost(), udpPacket.getFromPort()))).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        inboundChannel.close();
                    }
                });
                ReferenceCountUtil.release(msg);
            }
        } else {
            ReferenceCountUtil.release(msg);
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
        ctx.close();
    }
}