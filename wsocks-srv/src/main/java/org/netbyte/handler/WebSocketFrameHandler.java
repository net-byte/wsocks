package org.netbyte.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.netbyte.model.Config;
import org.netbyte.model.ProxyAddr;
import org.netbyte.utils.Cipher;
import org.netbyte.utils.SocksServerUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * WebSocketFrameHandler
 */
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private final static Logger logger = Logger.getLogger(WebSocketFrameHandler.class.getName());
    /**
     * Jackson object mapper
     */
    private final static ObjectMapper objectMapper = new ObjectMapper();
    /**
     * The config
     */
    private Config config;
    /**
     * Proxy address
     */
    private ProxyAddr proxyAddr;


    public WebSocketFrameHandler(Config config) {
        this.config = config;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            proxyAddr = decodeProxyAddr(((TextWebSocketFrame) frame).text());
            if (Objects.isNull(proxyAddr)) {
                logger.info("proxy address is null");
                SocksServerUtils.closeOnFlush(ctx.channel());
                return;
            }
            if (!Objects.equals(proxyAddr.getNetwork(), "tcp") && !Objects.equals(proxyAddr.getNetwork(), "udp")) {
                logger.info("network is wrong");
                SocksServerUtils.closeOnFlush(ctx.channel());
                return;
            }
            if (!Objects.equals(proxyAddr.getKey(), config.getKey())) {
                logger.info("key is wrong");
                SocksServerUtils.closeOnFlush(ctx.channel());
                return;
            }
            logger.info("proxy address " + proxyAddr.getNetwork() + " " + proxyAddr.getHost() + ":" + proxyAddr.getPort());
        } else if (frame instanceof BinaryWebSocketFrame) {
            if (Objects.isNull(proxyAddr)) {
                SocksServerUtils.closeOnFlush(ctx.channel());
                return;
            }
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener(
                    (FutureListener<Channel>) future -> {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            ctx.pipeline().addLast(new WebSocketProxyHandler(config, outboundChannel, proxyAddr));
                            ctx.pipeline().remove(this);
                        } else {
                            SocksServerUtils.closeOnFlush(ctx.channel());
                        }
                    });
            ByteBuf byteBuf = frame.content();
            try {
                Bootstrap b = new Bootstrap();
                if (Objects.equals(proxyAddr.getNetwork(), "tcp")) { // proxy tcp
                    b.group(ctx.channel().eventLoop())
                            .channel(NioSocketChannel.class)
                            .handler(new TcpProxyHandler(promise, ctx.channel(), config, ByteBufUtil.getBytes(byteBuf)))
                            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                            .option(ChannelOption.AUTO_READ, true)
                            .option(ChannelOption.SO_KEEPALIVE, true)
                            .option(ChannelOption.TCP_NODELAY, true);
                } else { // proxy udp
                    b.group(ctx.channel().eventLoop())
                            .channel(NioDatagramChannel.class)
                            .handler(new UdpProxyHandler(promise, ctx.channel(), config, ByteBufUtil.getBytes(byteBuf), proxyAddr))
                            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                            .option(ChannelOption.SO_BROADCAST, true)
                            .option(ChannelOption.AUTO_READ, true);
                }
                b.connect(proxyAddr.getHost(), Integer.parseInt(proxyAddr.getPort())).addListener(future -> {
                    if (!future.isSuccess()) {
                        SocksServerUtils.closeOnFlush(ctx.channel());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                SocksServerUtils.closeOnFlush(ctx.channel());
            }
        } else {
            SocksServerUtils.closeOnFlush(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    /**
     * Decode the proxy address from json data
     *
     * @param data
     * @return
     */
    private ProxyAddr decodeProxyAddr(String data) {
        try {
            if (Boolean.TRUE.equals(config.getObfs())) {
                return objectMapper.readValue(Cipher.xor(data.getBytes(StandardCharsets.UTF_8), config.getCipherKey()), ProxyAddr.class);
            }
            return objectMapper.readValue(data, ProxyAddr.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
