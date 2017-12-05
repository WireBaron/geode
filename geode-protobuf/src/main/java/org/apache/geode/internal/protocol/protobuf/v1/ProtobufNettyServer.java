/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.protocol.protobuf.v1;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import org.apache.geode.distributed.internal.InternalLocator;
import org.apache.geode.internal.admin.SSLConfig;
import org.apache.geode.internal.protocol.MessageExecutionContext;
import org.apache.geode.internal.protocol.state.ConnectionStateProcessor;
import org.apache.geode.internal.protocol.state.NoSecurityConnectionStateProcessor;
import org.apache.geode.internal.protocol.statistics.ProtocolClientStatistics;

public class ProtobufNettyServer {
  /**
   * The number of threads that will work on handling requests
   */
  private final int numWorkerThreads = 16;

  /**
   * The number of threads that will work socket selectors
   */
  private final int numSelectorThreads = 16;
  private final InternalLocator internalLocator;

  /**
   * The cache instance pointer on this vm
   */
  private SSLConfig sslConfig;

  /**
   * Channel to be closed when shutting down
   */
  private Channel serverChannel;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private static final int numExpirationThreads = 1;

  private boolean shutdown;
  private boolean started;

  private int port;
  private final boolean singleThreadPerConnection = false;

  private final ProtocolClientStatistics statistics;
  private final ProtobufStreamProcessor streamProcessor;
  private final ConnectionStateProcessor locatorConnectionState;

  public ProtobufNettyServer(int port, SSLConfig sslConfig, InternalLocator internalLocator,
      ProtocolClientStatistics statistics, ProtobufStreamProcessor streamProcessor) {
    this.port = port;
    this.sslConfig = sslConfig;
    this.internalLocator = internalLocator;
    this.statistics = statistics;
    this.streamProcessor = streamProcessor;
    this.locatorConnectionState = new NoSecurityConnectionStateProcessor();
  }

  public void run() throws IOException, InterruptedException {
    try {
      startServer();
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (!serverChannel.isOpen()) {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
      }
    }
  }

  private void startServer() throws IOException, InterruptedException {
    ThreadFactory selectorThreadFactory = new ThreadFactory() {
      private final AtomicInteger counter = new AtomicInteger();

      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("GeodeRedisServer-SelectorThread-" + counter.incrementAndGet());
        t.setDaemon(true);
        return t;
      }

    };

    ThreadFactory workerThreadFactory = new ThreadFactory() {
      private final AtomicInteger counter = new AtomicInteger();

      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("GeodeRedisServer-WorkerThread-" + counter.incrementAndGet());
        return t;
      }

    };

    bossGroup = null;
    workerGroup = null;
    Class<? extends ServerChannel> socketClass = null;
    if (singleThreadPerConnection) {
      bossGroup = new OioEventLoopGroup(Integer.MAX_VALUE, selectorThreadFactory);
      workerGroup = new OioEventLoopGroup(Integer.MAX_VALUE, workerThreadFactory);
      socketClass = OioServerSocketChannel.class;
    } else {
      bossGroup = new NioEventLoopGroup(this.numSelectorThreads, selectorThreadFactory);
      workerGroup = new NioEventLoopGroup(this.numWorkerThreads, workerThreadFactory);
      socketClass = NioServerSocketChannel.class;
    }
    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup).channel(socketClass)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {

            // SecurityManager
            // securityManager =
            // ((InternalCache) cache).getSecurityService().getSecurityManager();

            ChannelPipeline pipeline = ch.pipeline();

            // SSL
            // if(sslConfig.isEnabled())
            // pipeline.addLast("ssl", new SslHandler(makeSslEngine()));


            // Decoder
            pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());

            // Encoder
            pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
            pipeline.addLast("protobufEncoder", new ProtobufEncoder());

            // pipeline.addLast("authenticator", new ProtobufAuthenticatorHandler(securityManager));

            // pipeline.addLast(workerGroup, "authenticator", new Stream)
            pipeline.addLast("protobufDecoder",
                new ProtobufDecoder(ClientProtocol.Message.getDefaultInstance()));

            // Business Logic
            MessageExecutionContext executionContext =
                new MessageExecutionContext(internalLocator, statistics, locatorConnectionState);
            ChannelHandler channelHandler = new ChannelHandler(executionContext, streamProcessor);

            pipeline.addLast(workerGroup, "protobufOpsHandler", channelHandler);
          }
        }).option(ChannelOption.SO_REUSEADDR, true).option(ChannelOption.SO_RCVBUF, getBufferSize())
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

    // Bind and start to accept incoming connections.
    ChannelFuture f = b.bind(port).sync();
    this.serverChannel = f.channel();
  }

  // private SSLEngine makeSslEngine() {
  // SocketCreator socketCreator = new SocketCreator(sslConfig);
  // try {
  // SSLContext sslContext = socketCreator.createAndConfigureSSLContext();
  // SSLEngine sslEngine = sslContext.createSSLEngine();
  // sslEngine.setUseClientMode(false);
  // return sslEngine;
  // } catch (GeneralSecurityException | IOException e) {
  // throw new RuntimeException(e);
  // }
  // }

  private int getBufferSize() {
    return 65000;
  }

  public void close() {
    try {
      ChannelFuture closed = serverChannel.close();
      // shut down your server.
      closed.sync();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } finally {
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
    }
  }

  public class ChannelHandler extends ChannelInboundHandlerAdapter {
    private MessageExecutionContext executionContext;
    private final ProtobufStreamProcessor streamProcessor;

    public ChannelHandler(MessageExecutionContext executionContext,
        ProtobufStreamProcessor streamProcessor) {
      this.executionContext = executionContext;
      this.streamProcessor = streamProcessor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      ClientProtocol.Message message = (ClientProtocol.Message) msg;
      ClientProtocol.Message responseMessage =
          streamProcessor.handleMessage(message, executionContext);
      ctx.write(responseMessage);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      ctx.flush();
    }
  }
}
