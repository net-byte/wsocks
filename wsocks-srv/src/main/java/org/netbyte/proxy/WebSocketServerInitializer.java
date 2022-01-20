/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.netbyte.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.netbyte.handler.ConnectionCountHandler;
import org.netbyte.handler.WebSocketFrameHandler;
import org.netbyte.handler.WebSocketHttpHandler;
import org.netbyte.model.Config;
import org.netbyte.model.Counter;

import java.util.concurrent.Executors;

/**
 * WebSocketServerInitializer
 */
public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Config config;

    private final SslContext sslCtx;

    private final GlobalTrafficShapingHandler trafficHandler;

    public WebSocketServerInitializer(Config config, SslContext sslCtx) {
        this.config = config;
        this.sslCtx = sslCtx;
        this.trafficHandler = new GlobalTrafficShapingHandler(Executors.newScheduledThreadPool(1), 1000);
        Counter.trafficCounter = trafficHandler.trafficCounter();
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(trafficHandler);
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }
        pipeline.addLast(new ConnectionCountHandler());
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new WebSocketServerCompressionHandler());
        pipeline.addLast(new WebSocketServerProtocolHandler(config.getPath(), null, true));
        pipeline.addLast(new WebSocketHttpHandler());
        pipeline.addLast(new WebSocketFrameHandler(config));
    }
}
