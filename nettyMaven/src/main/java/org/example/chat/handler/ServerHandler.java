package org.example.chat.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.SneakyThrows;

import java.text.SimpleDateFormat;


public class ServerHandler extends SimpleChannelInboundHandler<String> {
    private static ChannelGroup channelGroup=new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //连接服务器时，自动触发
    @SneakyThrows
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel=ctx.channel();
        //将上线消息发送给channelGroup中的所有客户端，即对应的channel
        channelGroup.writeAndFlush("客户端 "+channel.remoteAddress()+
                "  上线了"+": "+sdf.format(new java.util.Date())+"\n");
        channelGroup.add(channel);
        System.out.println(ctx.channel().remoteAddress()+" 上线了"+"\n");

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)  {
        Channel channel=ctx.channel();
        channelGroup.writeAndFlush("客户端 "+channel.remoteAddress()+
                "  下线了"+": "+sdf.format(new java.util.Date())+"\n");
        channelGroup.remove(channel);
        System.out.println(ctx.channel().remoteAddress()+" 下线了"+"\n");
        System.out.println("当前在线用户有"+channelGroup.size());
    }

    //读取客户端的数据

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        Channel channel = ctx.channel();
        channelGroup.forEach(ch->{
            if(channel==ch){
                ch.writeAndFlush("自己发送了消息："+msg+"\n");
            }else{
                ch.writeAndFlush("客户端"+channel.remoteAddress()+
                        "发送了消息："+msg+"\n");
            }
        });
        System.out.println("客户端"+channel.remoteAddress()+"发送了消息： "+msg+"\n");
    }

    //数据读取结束后

}
