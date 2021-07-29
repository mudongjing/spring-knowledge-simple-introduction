package org.example.chat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.NettyRuntime;
import lombok.SneakyThrows;
import org.example.chat.handler.ServerHandler;

public class ServerChat {
    @SneakyThrows
    public static void main(String[] args){
        EventLoopGroup boosGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(8);
        //创建服务器端启动对象
        ServerBootstrap bootstrap=new ServerBootstrap();
        //配置参数
        bootstrap.group(boosGroup, workerGroup)//两个线程组
                .channel(NioServerSocketChannel.class)//服务器通道实现
                .option(ChannelOption.SO_BACKLOG,124)//队列大小
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    //通道对象初始化
                    @SneakyThrows
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        //workerGroup的通信通道设置处理器
                        ChannelPipeline pipeline=ch.pipeline();
                        pipeline.addLast("decoder",new StringDecoder());
                        pipeline.addLast("encoder",new StringEncoder());
                        pipeline.addLast(new ServerHandler());
                    }
                });
        System.out.println("聊天室服务器启动！");
        //绑定端口并同步
        ChannelFuture channelFuture=bootstrap.bind(7777).sync();
        channelFuture.channel().closeFuture().sync();
    }
}
