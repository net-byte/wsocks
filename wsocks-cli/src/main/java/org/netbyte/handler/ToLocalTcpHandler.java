package org.netbyte.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import org.netbyte.utils.Cipher;
import org.netbyte.utils.SocksServerUtils;


/**
 * Send data from remote websocket server to local socks5 server
 */
public final class ToLocalTcpHandler extends ChannelInboundHandlerAdapter {

    private final Channel inboundChannel;
    private final byte[] cipherKey;
    private final Boolean obfs;

    public ToLocalTcpHandler(Channel inboundChannel, byte[] cipherKey, Boolean obfs) {
        this.inboundChannel = inboundChannel;
        this.cipherKey = cipherKey;
        this.obfs = obfs;
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
                inboundChannel.writeAndFlush(Unpooled.copiedBuffer(xorData)).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        inboundChannel.close();
                    }
                });
                ReferenceCountUtil.release(byteBuf);
            } else {
                inboundChannel.writeAndFlush(binaryFrame.content()).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        inboundChannel.close();
                    }
                });
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