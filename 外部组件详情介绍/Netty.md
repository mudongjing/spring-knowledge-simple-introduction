# 介绍

Netty是致力于处理用户高并发请求的场景。很久以前的处理方法是，为用户建立一个普通的socket连接，与用户建立通信通道，并等待用户发送数据，这是一种阻塞方案，一个线程只能单一地等待消息发过来，无法处理其它任务。当然也可以为每个用户分出一个线程建立socket，那样就可以让主线程该干嘛干嘛，但对于上百万的用户数量而言，需要的硬件成本太高了。

因此，Java给出了无阻塞的通信方案，即NIO，主要的不同点在于将连接建立与等待数据作为两个任务处理，简单而言，连接建立完成，就放在一个队列中，至于如何读取用户的数据，不同时代也有不同的实现，

> - select：最早的，自然效率最差，它可以获悉有用户传入数据，但无法确定是谁，需要每次把所有的连接内容送入内核，有内核对存储的所有连接遍历一遍，而且最多支持1024个连接
> - poll：和select一个尿性，只是它的队列结构不同，可以支持任意多的连接
> - epoll：该机制，使用了事件驱动，当设备接受到消息后，该消息对应的事件类型等信息会对应的触发对应的连接，将该连接从原本的睡眠队列中，调到了一个就绪队列中，对于内核而言，是需要隔一段时间看一下就绪队列有没有连接即可，效率非常高。

java的nio提供了完整的方案，但终究具体的使用比较麻烦，实际上我们需要Netty帮我们简化。

> 如果，读者能完全了解java的nio体系，也可以对自己的使用场景做简化的实现，因此，下文将主要介绍nio的操作，顺便提及Netty是如何简化的。

# 设备信息

>  对于很多读者，估计习惯了各种框架的直接上手，对于如何用代码触碰自己使用的机器很是陌生。

这里，我们通过代码简单介绍一下，

```java
public class Device {
    @SneakyThrows//引入lombok依赖，可替代try catch
    public static void main(String[] args){
        //这里获取机器上的各种网络设备
        Enumeration<NetworkInterface> netEn=NetworkInterface.
                getNetworkInterfaces();
        while(netEn.hasMoreElements()){
            //这个得到的设备的对象，将包含该设备的各种信息
            NetworkInterface networkInterface=netEn.nextElement();
            Enumeration<InetAddress> inetAddresses = networkInterface.
                    getInetAddresses();
            byte[] hardwareAddress = networkInterface.getHardwareAddress();
            System.out.println("设备名："+networkInterface.getName()+
                    "\n 设备名称显示："+networkInterface.getDisplayName()+
                    "\n 网络接口索引："+networkInterface.getIndex()+
                    "\n 是否开启并运行："+networkInterface.isUp()+
                    "\n 最大传输单元："+networkInterface.getMTU()+
                    "\n 是否是p2p设备："+networkInterface.isPointToPoint()+
                    "\n 是否为回调接口："+networkInterface.isLoopback()+
                    "\n 是否支持多播："+networkInterface.supportsMulticast()+
                    "\n ip");

            if(!inetAddresses.hasMoreElements()) System.out.println("   无ip");
            while(inetAddresses.hasMoreElements()){
                InetAddress inetAddress = inetAddresses.nextElement();
                System.out.println("    "+"主机名："+inetAddress.getHostName()+
                        "\n"+"    "+"IP地址："+inetAddress.getHostAddress());
            }

            System.out.println(" 硬件地址："+byte2str(hardwareAddress));
            System.out.println("===========================");
        }
    }
    @SneakyThrows
    private static String byte2str(byte[] by){
        if(by!=null){
            StringBuffer sb = new StringBuffer();
            for(int i=0;i<by.length;i++){
                if(i!=0){
                    sb.append("-");
                }
                //mac[i] & 0xFF 是为了把byte转化为正整数
                String s = Integer.toHexString(by[i] & 0xFF);
                sb.append(s.length()==1?0+s:s);
            }
            return sb.toString().toUpperCase();
        }
        return " --无";
    }
}
```

> 大致的结果，部分如下，~~这里使用的是windows系统，如果是linux，那么名称可能有所不同~~
>
> ```bash
> 设备名：lo
>  设备名称显示：Software Loopback Interface 1
>  网络接口索引：1
>  是否开启并运行：true
>  最大传输单元：-1 #-1 说明此时该设备可能禁用
>  是否是p2p设备：false
>  是否为回调接口：true #这里显示为回调接口，因此127.0.0.1归它
>  是否支持多播：true
>  ip
>     主机名：licensing.ultraedit.com
>     IP地址：127.0.0.1
>     主机名：0:0:0:0:0:0:0:1
>     IP地址：0:0:0:0:0:0:0:1
>  硬件地址： --无
> ===========================
> 设备名：net3
>  设备名称显示：Microsoft Teredo Tunneling Adapter
>  网络接口索引：12
>  是否开启并运行：true
>  最大传输单元：1280 # 不同的设备，对应的最大传输单元也是不同的。单位字节。
>  #以太网网卡大多默认1500字节，IPv6下，范围在1280~65535
>  是否是p2p设备：true
>  是否为回调接口：false
>  是否支持多播：false
>  ip
>     主机名：DESKTOP-3NVQLN7
>     IP地址：2001:0:348b:fb58:10b2:ae17:2c75:8b14
>     主机名：DESKTOP-3NVQLN7
>     IP地址：fe80:0:0:0:10b2:ae17:2c75:8b14%net3 #这里说明一个设备可以对应着多个ip地址
>  硬件地址：00-00-00-00-00-00-00-E0
> ===========================
> ```
>
> > 此外linux有一种特有的虚拟接口，即正常的设备可以有多个虚拟设备，可惜我的linux设备没有显示出来，
> >
> > ```java
> > Enumeration<NetworkInterface> subInterfaces = networkInterface.getSubInterfaces();
> > while(subInterfaces.hasMoreElements()){
> >     NetworkInterface networkInterface1 = subInterfaces.nextElement();
> >     System.out.println("");
> >     System.out.println("设备名："+networkInterface1.getName()+
> >                        "\n 设备名称显示："+networkInterface1.getDisplayName()+
> >                        "\n 是否为虚拟接口"+networkInterface1.isVirtual()+
> >                        "\n 父接口哈希值"+networkInterface1.getParent().hashCode()
> >                       );
> > }
> > ```

# 通信

对于通信，我们都知道是使用tcp或udp协议。

对于需要双方的可靠通信，我们自然需要使用tcp，基本上对于需要客户端与服务端建立通信的，也经常是这种方式。而建立的连接也有长短区别，短连接就是发送完数据就断开，而长连接则是需要长久保持的，例如我们经常搞的数据库连接等就属于长的。

## BIO

相比于本文Netty中使用的NIO，BIO则是非常传统的IO模式，它具有明显的阻塞特性，无论是建立连接，还是写入数据，都需要有对应的动作发生，才能进行下一步操作。

### 连接

我们这里先简单地给一个阻塞的连接建立，

```java
public class ServerBio {//建立一个服务器的连接
    @SneakyThrows
    public static void main(String[] args){
        ServerSocket socket=new ServerSocket(自定义一个端口);//ip就是本机地址，或回调地址
        //即，既可以使用127.0.0.1，也可以看一下你的机器在内网或公网的ip
        System.out.println("server 发生阻塞");
        socket.accept();//
        System.out.println("server建立了连接，结束阻塞");
        socket.close();
    }
}
```

到这里基本就可以了，启动它，就会发现，程序停在阻塞哪里，

> 为了方便，可以直接在命令窗口中，使用
>
> ```bash
> telnet ip 端口 # 这就相当于建立了发出了建立连接的请求
> ```
>
> > 在windows上可以使用命令确认一下，自己使用的端口没有与其它程序有冲突
> >
> > ```dash
> > netstat -ano |findstr "端口号"
> > ```
> >
> > linux可以使用
> >
> > ```bash
> > lsof -i:端口号
> > #或者
> > netstat -tunlp | grep 端口
> > ```

> 也可以创建对应的客户端类
>
> ```java
> public class ClientBio {
>     @SneakyThrows
>     public static void main(String[] args){
>         System.out.println("试图建立连接");
>         Socket socket=new Socket("localhost",80);//也可以写对应的网址
>         System.out.println("连接建立完成");
>         socket.close();
> 
>     }
> }
> ```

### 数据

同样的数据读取也会有一个阻塞，

对应的server代码

```java
public class ServerDataBio {
    @SneakyThrows
    public static void main(String[] args){
        char[] chars=new char[3];//这里指定每次读取的信息量
        ServerSocket serverSocket=new ServerSocket(端口);
        System.out.println("开始阻塞");
        Socket socket = serverSocket.accept();
        System.out.println("连接建立");
        InputStream inputStream=socket.getInputStream();
        InputStreamReader inputStreamReader=new InputStreamReader(inputStream);
        System.out.println("等待数据");
        
        //这里按照给定的大小读取数据，即先读取3个字符
        int readLength=inputStreamReader.read(chars);
        while(readLength!=-1){
            String str=new String(chars,0,readLength);
            System.out.println("data::"+str);
            readLength=inputStreamReader.read(chars);
        }
        System.out.println("数据读取结束");
        inputStreamReader.close();
        inputStream.close();
        socket.close();
        serverSocket.close();
    }
}
```

客户端

```java
public class ClientDataBio {
    @SneakyThrows
    public static void main(String[] args){
        Socket socket=new Socket("localhost",端口);
        System.out.println("建立了连接");
        System.out.println("开始休眠3s");
        Thread.sleep(3000);
        OutputStream outputStream=socket.getOutputStream();
        System.out.println("发送数据");
        outputStream.write("我在发送一些信息".getBytes());
        outputStream.close();
        socket.close();
    }
}
```

> 大概的结果如下
>
> ```bash
> server:												  client:
> 开始阻塞												建立了连接		
> 连接建立 												开始休眠3s
> 等待数据 												发送数据
> data::我在发
> data::送一些
> data::信息
> 数据读取结束
> ```
>
> 类似地，服务端也可以用同样的方法像客户端发送数据。
>
> 只要outputStreamReader不被关闭，就可以多次使用write()写入数据进行发送。

到这里，读者就能明显地感觉到，虽然传统的通信方式满足了通信的基本需求，但非常需要对方及时的配合，否则，整个程序的运行将断断续续。

更多关于socket的操作，如传输图片，udp/tcp通信，缓存，远程端口获取、本地端口复用，绑定，ip获取，多线程通信等，可以在附录的[socket简单使用](#socket简单使用)中查看。

## NIO

> 首先说明一下Linux中一些系统机制，Linux中将一切都尽可能地视为文件，所有的设备，包括网络连接都被用文件来表示。
>
> 比如，对一个文件的操作会调用的内核的系统命令，并返回一个文件描述符（file descriptor，fd），而一个对socket的读写，也会返回一个socket文件描述符（socketfd)，Linux底层中有c语言实现了对应的结构体负责存储这些信息，通过对应的各种fd，可以在对应的结构体中获取对应的详细信息。
>
> 其中I/O在系统中包含 几种处理模型，比如基于缓存的非阻塞模型，或是信号驱动模型。这里介绍的NIO，虽然名称上是叫非阻塞IO，但实际是建立在I/O服用模型上，
>
> > 前面提及过的select或者poll，就是Linux的系统调用命令，产生的连接就是转化为对应fd，并放入结构体中，select或poll就是单纯地遍历扫描所有fd的状态，而epoll基于事件驱动机制，当对应的fd就绪，会自动触发其回调函数rollback。
>
> > 而所谓的复用技术，就是体现在对所有fd的扫描或事件驱动上。此时，一个线程不再是专注于一个连接，而是统一起来，由一个或多个线程共同负责，
> >
> > > 就类似于原先一个母亲全心照顾一个孩子，除了孩子的事情，基本做不了其它工作。而现在，可以把孩子放在托儿所，一个老师管理一群孩子，到睡觉事件就一起睡，谁要上厕所就打个报告，老师带你去。
> >
> > 这样的一个另一个好处在于，不同连接之间的数据交流不再需要费力地从一个进程到另一个进程，老师就能直接和各个父母说，你家谁谁跟谁谁搞什么事了。
> >
> > > 另外，由于对于连接的这些处理都必须将对应的fd放入内核中处理，就导致需要把处理的结果再从内核的内存中转移到用户的内存中，epoll则将这个消息数据区域的内存设置为内核与用户共享的区域。
> > >
> > > > 就相当于，原本放学后，老师得一个个找对应的父母说一下今天孩子的状态，但现在，老师直接把每个小孩的情况写在对应的本子上，父母直接过来拿对应的本子看一下就OK了。

> ~~最后补充一个无用的知识，如果管理的连接都是非常活跃的，那么使用epoll命令反而降低效率，不如直接使用poll。~~

通过上述的介绍，我们可以知道，所谓的NIO，就是在系统级别上对连接的统一管理，那么在编程方面，就需要有一个对象是代表这样的管理者，有了管理者，自然也需要有被管理的员工。

管理者就是选择器（Selector），员工就是通道（Channel）。

> 选择器代表的就是负责调度连接的线程，至于线程数则是看实际的连接数量动态调整。默认是，1023个连接就增加一个线程。

通道可以读写数据，而我们还需要额外的一个缓冲区对象（Buffer）负责安放我们操作的数据。

简单实现一个，

```java
public class ServerChannel {
    private static List<SocketChannel> channelList=new ArrayList<>();
    @SneakyThrows
    public static void main(String[] args){
        //用于监听客户端连接，是所有客户端连接的父管道
        ServerSocketChannel serverSocketChannel=ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);//设置为非阻塞
        //这里的2,对应的是挂起来的最大待处理连接的数量，也就是当一条连接处理稍微慢一些，而连接的数量又很大
        //此时，如果太多的话，超过我的设定，程序就会报异常。看自己的机器性能好坏，可以提高数量。
        serverSocketChannel.bind(new InetSocketAddress("localhost",端口号),2);
        //获得一个选择器
        Selector selector= Selector.open();
        //这里注册指定的选择器并指定类型。当然也可以获取多个选择器并注册为不同类型。
        //这里对应的是accept，即负责接受新建立的连接
        //其它的还有例如`OP_READ`，负责处理当前连接中发来的数据
        //当有对应的事件发生，将首先注册到该选择器中，等待后续的统一处理
        SelectionKey key=serverSocketChannel.register(selector,SelectionKey.OP_ACCEPT);
        System.out.println("准备开始");

        while(true){
            System.out.println("进入阻塞");
            //这里会处于阻塞状态，直到出现符合该选择器类型的动作出现，避免无意义的空转
            //更底层的epoll,实际上就是硬件设备有对应的电信号才会通过触发对应的回调函数，而结束阻塞
            selector.select();
            //获取该选择器中注册事件类型的SelectionKey实例
            Set<SelectionKey> selectionKeySet=selector.selectedKeys();
            Iterator<SelectionKey> selectionKeyIterator=selectionKeySet.iterator();
            //对发生的事件进行处理
            while(selectionKeyIterator.hasNext()){
                SelectionKey key1=selectionKeyIterator.next();
                //这里判断一下是否真的就是 OP_ACCEPT 事件
                if(key1.isAcceptable()){
                    ServerSocketChannel serverSocketChannel1=(ServerSocketChannel) key1.channel();
                    SocketChannel socketChannel = serverSocketChannel1.accept();
                    socketChannel.configureBlocking(false);
                    //上述获得了一个新的的连接，接下来就是把它在注册一遍，但是需要是read类型
                    SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("客户端建立成功");
                }else if(key1.isReadable()){//如果是一个存在的连接，必然是传入了数据
                    SocketChannel socketChannel=(SocketChannel)key1.channel();
                    ByteBuffer byteBuffer=ByteBuffer.allocate(64);
                    int len=socketChannel.read(byteBuffer);
                    if(len>0){
                        System.out.println("接收的消息"+new String(byteBuffer.array()));
                    }else if(len==-1){
                        socketChannel.close();
                        System.out.println("该客户端已断开连接");
                    }
                }
                //清除迭代器的最后一个元素，否则select会以为最后一个没处理，又搞一遍
                selectionKeyIterator.remove();
        }
    }
}
```

这里的代码基本上，完成了一个NIO的主要结构，我们可以多弄几个 `telnet`命令进行连接，看一下效果。

进一步的，我们会发现，这里实际就是一个线程，既负责建立连接，又负责处理数据，而之前我们看到的[Netty](https://github.com/mudongjing/spring-knowledge-simple-introduction/blob/main/Netty.md)

中，是做了分工的。其实，这种分工的模式就如下面这个图中显示的

![][分工]

这幅图来自[scalable IO in Java][scala]，作者就是java的各种并发包的主要作者。图中指出的就是，让一个小组专门负责建立新连接，而已建立的连接则交给另一个小组，由一组线程共同负责读取等操作。

而更具体的，我们代码是实现的流程如下图

![][架构]

# Netty

经过上述的一番介绍后，我们基本重新了解了一遍java代码如何完成网络连接，但很明显，纯粹的java代码使用起来太过麻烦。

外面的Netty文档中，我们给出了一个简单的聊天室功能的实现，而上面的代码显示，只是完成一个简单的NIO的连接并读取数据，就耗费了不少啰嗦的代码，更何况，还要进行线程分工，分连接发送数据。











零拷贝：是将我们的数据直接放到对应的磁盘文件中，

> 而不是先IO把磁盘中对应的数据拿到用户内存空间，在调到系统内存空间，一番操作，再调用IO放入磁盘文件中

前提是，我们需要加入的数据是不需要使用磁盘中的数据，即我们知识单向地修改磁盘中的数据，那么，我们就调用sendfile函数，指明磁盘中那个文件，准备好我们自己的数据，指定磁盘文件存放数据的位置起始点，一次调用IO，之间塞进去。













# 附录

## socket简单使用



## NIO的其它操作



## Channel的分类及使用



## AIO



































[分工]:https://mudongjing.github.io/gallery/netty/ffgs.png
[scala]:http://gee.cs.oswego.edu/dl/cpjslides/nio.pdf	"scalable IO in Java"
[架构]:https://mudongjing.github.io/gallery/netty/jdgb.png

