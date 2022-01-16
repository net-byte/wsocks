package org.netbyte.handler;

import io.netty.channel.*;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import org.netbyte.model.Config;
import org.netbyte.utils.SocksServerUtils;

/**
 * The socks5 udp associate handler
 */
public final class UdpAssociateHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private final Config config;

    public UdpAssociateHandler(Config config) {
        this.config = config;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) {
        if (!(message instanceof Socks5CommandRequest)) {
            ctx.fireChannelRead(message);
            return;
        }
        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                Socks5CommandStatus.SUCCESS,
                Socks5AddressType.IPv4,
                config.getLocalhost(),
                config.getLocalPort())).addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                SocksServerUtils.closeOnFlush(ctx.channel());
            }
        });
        ctx.fireChannelRead(message);
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