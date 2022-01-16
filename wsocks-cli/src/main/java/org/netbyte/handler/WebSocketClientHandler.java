package org.netbyte.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import org.netbyte.model.ProxyAddr;
import org.netbyte.model.UdpPacket;
import org.netbyte.utils.Cipher;
import org.netbyte.utils.SocksServerUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * The websocket client handler
 */
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketClientHandshaker handshaker;
    private final Promise<Channel> promise;
    private final Channel inboundChannel;
    private final ProxyAddr proxyAddr;
    private final byte[] cipherKey;
    private final Boolean obfs;
    private UdpPacket udpPacket;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker, Promise<Channel> promise, Channel inboundChannel, ProxyAddr proxyAddr, byte[] cipherKey, Boolean obfs) {
        this.handshaker = handshaker;
        this.promise = promise;
        this.inboundChannel = inboundChannel;
        this.proxyAddr = proxyAddr;
        this.cipherKey = cipherKey;
        this.obfs = obfs;
    }

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker, Promise<Channel> promise, Channel inboundChannel, ProxyAddr proxyAddr, byte[] cipherKey, Boolean obfs, UdpPacket udpPacket) {
        this.handshaker = handshaker;
        this.promise = promise;
        this.inboundChannel = inboundChannel;
        this.proxyAddr = proxyAddr;
        this.cipherKey = cipherKey;
        this.obfs = obfs;
        this.udpPacket = udpPacket;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpResponse)) {
            ctx.fireChannelRead(msg);
            return;
        }
        FullHttpResponse response = (FullHttpResponse) msg;
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ctx.channel(), response);
            } catch (WebSocketHandshakeException e) {
                e.printStackTrace();
                return;
            }
            // send proxy address to server
            String proxyAddress = objectMapper.writeValueAsString(this.proxyAddr);
            if (Boolean.TRUE.equals(obfs)) {
                proxyAddress = new String(Cipher.xor(proxyAddress.getBytes(StandardCharsets.UTF_8), cipherKey));
            }
            ctx.writeAndFlush(new TextWebSocketFrame(proxyAddress)).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    if (Objects.equals(proxyAddr.getNetwork(), "tcp")) {
                        ctx.pipeline().addLast(new ToLocalTcpHandler(this.inboundChannel, this.cipherKey, this.obfs));
                        ctx.pipeline().remove(this);
                        promise.setSuccess(ctx.channel());
                    } else {
                        byte[] data = udpPacket.getData();
                        if (Boolean.TRUE.equals(obfs)) {
                            data = Cipher.xor(data, cipherKey);
                        }
                        ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(data))).addListener((ChannelFutureListener) f -> {
                            if (f.isSuccess()) {
                                ctx.pipeline().addLast(new ToLocalUdpHandler(this.inboundChannel, this.cipherKey, this.obfs, this.udpPacket));
                                ctx.pipeline().remove(this);
                                promise.setSuccess(ctx.channel());
                            }
                        });
                    }
                } else {
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            });
        } else {
            response.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        promise.setFailure(cause);
        ctx.close();
    }
}