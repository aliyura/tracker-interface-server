package com.kobo360.trackerinterface;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;

public class Server {

  private final int port;
  private static final Logger logger = Logger.getLogger(MyServerHandler.class);

  public Server(int port) {
    this.port = port;
  }

  public void start() throws Exception {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup group = new NioEventLoopGroup();
    try {
      ServerBootstrap sb = new ServerBootstrap();
      sb.option(ChannelOption.SO_BACKLOG, 1024);
      sb.group(group, bossGroup).channel(NioServerSocketChannel.class).localAddress(this.port).childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          logger.info("-------------有客户端连接--------------");
          logger.info("IP:" + ch.localAddress().getHostName());
          logger.info("Port:" + ch.localAddress().getPort());
          ch.pipeline().addLast(new MyDecoder());
          ch.pipeline().addLast(new MyServerHandler());
        }
      });
      ChannelFuture cf = sb.bind().sync();
      System.out.println("socket服务端启动成功，监听端口： " + cf.channel().localAddress());
      cf.channel().closeFuture().sync();
    } finally {
      group.shutdownGracefully().sync();
      bossGroup.shutdownGracefully().sync();
    }
  }

  public static void main(String[] args) throws Exception {
    new Server(8888).start();
  }
}