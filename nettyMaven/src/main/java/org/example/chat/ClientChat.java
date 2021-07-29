package org.example.chat;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import org.example.chat.handler.ClientHandler;

import java.util.Scanner;


public class ClientChat {
    @SneakyThrows
    public static void main(String[] args){
        //客户端需要事件循环组
        EventLoopGroup eventLoopGroup=new NioEventLoopGroup();
        Bootstrap bootstrap=new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch)  {
                        ChannelPipeline pipeline=ch.pipeline();
                        pipeline.addLast("decoder",new StringDecoder());
                        pipeline.addLast("encoder",new StringEncoder());
                        pipeline.addLast(new ClientHandler());
                    }
                });
        System.out.println("客户端启动");
        //连接服务器
        ChannelFuture channelFuture=bootstrap.connect("127.0.0.1",7777).sync();
        Channel channel=channelFuture.channel();
        System.out.println("====="+channel.localAddress()+"========");
        // 创建扫描器，录入消息
        Scanner scanner=new Scanner(System.in);
        while(scanner.hasNextLine()){
            String msg=scanner.nextLine();
            channel.writeAndFlush(msg);
        }


        channelFuture.channel().closeFuture().sync();//关闭通道并监听
    }
}
