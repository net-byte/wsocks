package org.netbyte.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import org.netbyte.utils.Cipher;
import org.netbyte.utils.SocksServerUtils;

/**
 * Send data from local socks5 server to remote websocket server
 */
public final class ToServerTcpHandler extends ChannelInboundHandlerAdapter {

    private final Channel websocketChannel;
    private final byte[] cipherKey;
    private final Boolean obfs;

    public ToServerTcpHandler(Channel websocketChannel, byte[] cipherKey, Boolean obfs) {
        this.websocketChannel = websocketChannel;
        this.cipherKey = cipherKey;
        this.obfs = obfs;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!websocketChannel.isActive()) {
            ReferenceCountUtil.release(msg);
            return;
        }
        ByteBuf byteBuf = (ByteBuf) msg;
        if (Boolean.TRUE.equals(obfs)) {
            byte[] xorData = Cipher.xor(ByteBufUtil.getBytes(byteBuf), cipherKey);
            websocketChannel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(xorData)));
            ReferenceCountUtil.release(byteBuf);
        } else {
            websocketChannel.writeAndFlush(new BinaryWebSocketFrame((ByteBuf) msg));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SocksServerUtils.closeOnFlush(websocketChannel);
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}