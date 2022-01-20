package org.netbyte.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.netbyte.model.Counter;


/**
 * Count connections
 */
public class ConnectionCountHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext context){
        Counter.totalConnections.getAndIncrement();
        Counter.currentConnections.getAndIncrement();
    }
    @Override
    public void channelInactive(ChannelHandlerContext context){
        Counter.currentConnections.getAndDecrement();
    }
}