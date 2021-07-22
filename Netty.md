> **Netty**是 一个**异步事件驱动**的网络应用程序框架，用于**快速开发可维护的高性能协议服务器和客户端**。

相当于是NIO的替代。linux中称之为non-blocking IO，即非阻塞IO，而java中目标一样，但是就是new IO，因为以前的普通io，就是BIO，是会阻塞的。。

> 普通的BIO是当我们的服务器试图处理可能的连接时，该线程会一直等待，直到出现一条连接进来，随后，该线程可能又会一直等待着该连接传入一些消息，中间这些环节，如果没有服务器其它的情况发生，线程就会堵塞而无法处理其它事务。
>
> 由此，就出现了NIO，很明显，就是它不再傻傻地堵塞着在那里等待，对于我们而言，主要关心linux的环境，而java的nio在linux环境下就会调用系统的epoll等系统函数，如`epoll_create`、`epoll_wait`。当用户与linux服务建立了联系后，首先会在对应的网络端口上有触发，在硬件上会产生电信号，并传入CPU的一个引脚，从而触发一个系统中断，逼迫CPU停下当前的工作来处理这个网络的触发事件。
>
> > 同样的，即使我们建立了连接，而之后，我们并没有发送信息，服务器的端口也不会被触发。反过来说，如果系统的网络端口不被触发，那么就代表没有连接或消息进入。
>
> 于是，通过底层的系统函数，就可以直到当前是否有额外的事件发生，没有的话，自然就等待着，
>
> > 不然，按照以往的方法，如jdk以前的做法select或poll，都需要不停地循环遍历建立的连接，非常低效。
>
> 当发现了新连接，就会调用系统函数，将其主要标志纳入到一个结构体中，当再次发生事件触发，并发现是已有的连接，发送消息，那么就直接提取该连接并处理消息，避免了额外的操作。

当一个服务器需要处理大量的连接时，如大量用户进入一个网络聊天室，或游戏玩家同时在线操作，这些场景可能都会达到百万人数或更多的级别，显然单纯的多线程是没有用的，即使服务器的内存足够，普通的多线程需要不停地遍历数百万的连接。此时就需要使用读写分离的手段，读者可能觉得这不是数据库集群的操作嘛。

> 在这里，建立新连接就可以看作是写，而已有的连接发送消息，对我们而言就类似于读。与其每个用户设定一个线程处理连接，还不如一个线程处理一批用户。
>
> 可以设定一个连接负责接收新连接，一个线程处理线程发送的消息，还是nio的通信，对于一个线程而言，只有管理的连接有动静才会操作。但如果是游戏服务器，可能用户比较活跃，那么多搞几个线程，实现类似一主多从的模式，只需要几个线程，基本就能处理较为庞大的使用场景。
>
> > 插个题外话，以前Redis单线程的时候，一样可以处理外部大量的连接，就是因为使用epoll系统函数。

上述的介绍，说明了NIO的优秀，但众所周知，java的好多功能，如果让我们直接使用，那无异于慢性自杀，因此，Netty就是简化了NIO的操作，将我们试图用NIO实现的功能提前封装完成。

# 基本操作

现在，我们就简单使用一下，建立一个maven项目，引入`netty-all`依赖，我们简单实现一个聊天室的功能，首先需要一个服务器进行消息的接收和转发，然后再实现客户端发送消息和接收服务端信息的功能。

服务端

```java
public class ServerChat {
    @SneakyThrows//这是lombok的注解，具有try catch的的功能
    public static void main(String[] args){
        EventLoopGroup boosGroup = new NioEventLoopGroup(1);//括号里指定的是线程数
        EventLoopGroup workerGroup = new NioEventLoopGroup(8);
        //上面两个就是希望完成读写分离的手段，线程数可以自定义
        
        //创建服务器端启动对象
        ServerBootstrap bootstrap=new ServerBootstrap();
        //配置参数
        bootstrap.group(bossGroup, workerGroup)//两个线程组，分别对应着连接的建立和内容的读取
            //如果你只希望有一个完成，两个参数也可以是相同的
            //这里我们实现的是boss的一个线程负责建立连接，worker的则是内容，
			            //这样的分工状态，boss不管怎样，都只会使用一个线程【限于绑定一个端口】
            //如果读者只打算用boss完成全部工作，可以将对应的线程数提高一些，
            			//这样，仍会自动划分出一个线程负责连接，而其它线程负责内容
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
                        //这个handler需要自己实现，名字随意
                    }
                });
        System.out.println("聊天室服务器启动！");
        //绑定端口并同步
        ChannelFuture channelFuture=bootstrap.bind(7777).sync();//端口号随意
        //这里我们简单地绑定了一个端口，因此，此时负责连接的线程一个就足够了
        //如果试图绑定多个端口，那么此时就可以对boss设置多线程，负责多个端口的监听
        channelFuture.channel().closeFuture().sync();
    }
}
```

ServerHandler，

```java
public class ServerHandler extends SimpleChannelInboundHandler<String> {
    //用于存储上线客户端的通信通道
    private static ChannelGroup channelGroup=new 																		DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
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
        //msg室netty替我们把客户端发送的消息转化成string
        Channel channel = ctx.channel();//获取与当前客户端的那个通信通道
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
}
```

> 此时在IDEA的IDE中运行ServerChat的main函数，运行窗口就会显示 `聊天室服务器启动！`

客户端，

```java
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
                        pipeline.addLast(new ClientHandler());//也要自己实现，名字随意
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
```

ClientHandler，

```java
public class ClientHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println(msg.trim()+"\n");//打印出服务发送的消息
    }
}
//内部有很多方法可以实现，但这里，我们就只是简单地获取服务器的消息即可
```

> 此时启动这个客户端，服务端那边就会显示出上线提示。为了获得多个客户端，可以在运行的设置中，调整为允许并行。
>
> ![][设置并行]













[设置并行]:https://z3.ax1x.com/2021/07/20/WtmmPP.png



