## RabbitMQ介绍

>  作为消息队列，也可以称为消息中间件，主要是以异步的方式进行消息传递，一方作为生产者向队列填充消息，另一方从队列中不断获取消息，就是这么简单的功能。

### 1. 安装

具体的安装方式在官网对不同的系统都有介绍，我这个时间段最新的是3.8.19。我们就以最新的版本为例，系统为Centos8。

首先安装Erlang，因为RabbitMQ就是这个语言写的，可以使用rabbitmq提供的一个erlang的[安装包](https://github.com/rabbitmq/erlang-rpm)，这是在github上的项目，只包含了rabbitmq所需的包，相比大多数读者不太会过多的使用Erlang，推荐使用这个，下载一个最新的就行，我这里最新的版本是[24.0.3](https://github.com/rabbitmq/erlang-rpm/releases/tag/v24.0.3)。放到系统中，yum安装就好。

然后是下载rabbitmq的[rpm文件](https://github.com/rabbitmq/rabbitmq-server/releases/download/v3.8.19/rabbitmq-server-3.8.19-1.el8.noarch.rpm)，用yum安装，在这之前，先检查一下自己的centos系统中是否有socat 、logrotate，没有就安装。如果你的系统挺简洁的，可能还会遇到其它组件缺失的情况，慢慢装完就好。

这里组件尽量都尝试在linux上安装，最好别在windows上装，macOS安装也没什么问题。

### 2. 配置

以前的版本，可能会提供一个类似rabbitmq.config.example的文件方便我们修改为rabbitmq.config，但是当前而言，没有example文件，对应的文件也变成了rabbitmq.conf。另外还配备了文件advanced.config，同样安装之后并不会为我们提供示例文件，但可以在对应的github项目中的[docs](https://github.com/rabbitmq/rabbitmq-server/tree/v3.8.x/deps/rabbit/docs)中找到。但目前这些配置文件还不需要。

RabbitMQ在CentOS8系统下的启动之类的方法是【其它系统官网也有介绍】

```bash
systemctl start rabbitmq-server
systemctl stop rabbitmq-server
systemctl restart rabbitmq-server
systemctl status rabbitmq-server
```

为了能够在浏览器中访问rabbitmq的信息，则需要启动它的一个插件

```bash
rabbitmq-plugins enable rabbitmq_management#如果要关闭，改成disable即可
```

但一般情况下，我们都设置了防火墙，有的教程直接叫你关了防火墙（太暴力的）,现在为了浏览器访问我们只需要15672端口，那开放就好了（~~如果是云服务器，可还能需要在对应的安全规则中开放对应的端口~~）

```bash
firewall-cmd --zone=public --add-port=15672/tcp --permanent
firewall-cmd reload #重启生效
```

此时，在其它地方的浏览器上，输入`http://对应的主机IP:15672`即可访问对应的页面。

但此时，是一个登录页面，而且默认登录名和登录密码都是`guest`，但这一账户的登录仅限于在它的本地主机登录使用，其它地方是无法使用的。

此时我们就增加我们自己的用户

```bash
rabbitmqctl list_users #我们可以先看看当前已有的用户，只有guest
rabbitmqctl add_user 用户名 密码
rabbitmqctl set_user_tags 职位
#对应的tags，目前有management、policymaker、monitoring、administrator
#权限递增
```

想省事，直接设置为administrator，稍微低调点设置成monitoring也就差不多了，具体各职位的权限在[官网](https://www.rabbitmq.com/management.html#permissions)可看，在对应的官网页面上也可以看到设置其它权限的方法。总之我们现在有一个有较高权限的用户就够了，此时就可以登录对应的页面了。

### 3. rabbitmq的基本情况

前面的简单操作已经可以基本完成了rabbitmq的启动。

作为消息队列应用，【以后RabbitMQ简化为RMQ】，RMQ内部首先可以包含大量的队列，我们可以为队列命名，此外，我们的应用可能包含几个负责产生消息的，另外有几个是负责接收消息的。

> 类似于双十一，我们的手机上的客户端，在我们点击商品购买后，则产生了一条消息，并发送到一个队列中，之后手机的应用该干嘛还干什么，对应的服务器抽出空了，从这个队列中将消息全都提取出来，并判断那些是秒杀的，再将对应的信息显示给我们。
>
> 这样一方面避免了耦合，另一方面提高了吞吐量，因为消息产生并发送出去就不用干等着了。
>
> 当然了，阿里用的消息队列是自己搞得的roketmq，基本是kafka的模仿提升版，这类消息队列的特点是就是快，数据之类的可以丢失，但动作不能慢，就是为了应对这样的大数据情况。而面现在的rabbitmq就显得中庸了一些，毕竟正常环境还是数据安全点好。

RMQ内部可以设置多个虚拟机，用以针对不同权限的用户。虚拟机内部有包含了交换机和队列，正常我们直接将消息传递给指定的队列即可，但是如果队列很多，需求复杂则需要额外考虑一些因素，而这些考虑就可以交给交换机来完成，它会将我们的消息按照一定的规则发送到对应的队列中。

虚拟主机的操作[官网](https://www.rabbitmq.com/vhosts.html)也有介绍，

```bash
rabbitmqctl add_vhost 自定义主机名#创建一个虚拟主机
rabbitmqctl list_vhosts name#可查看当前具有的虚拟主机
rabbitmqctl delete_vhost 主机名 #删除
#再结合设置权限的操作
set_permissions [-p vhost] user conf write read #官网介绍为用户指定在对应虚拟主机的权限
#官网示例，该命令指示 RabbitMQ broker 授予名为“janeway”的用户访问名为“my-vhost”的虚拟主机，对名称以“janeway-”开头的所有资源具有配置权限，并对其进行读写权限所有资源：
rabbitmqctl set_permissions -p my-vhost janeway "^janeway-.*" ".*" ".*"
#但现在，我们还没有什么特别的资源，因此，后面的资源都写为 ".*"即可
```

而交换机（Exchage）和队列（queue）的创建则可以在对应的浏览器页面上操作。其中交换机有多种类型，具体的不同到时候再说。

此外，队列可以有多个消息生产者，也可以有多个消息消费者。

而这类需要面对较大应用场景的软件，为了保证数据的安全性，它也具有集群机制，可通过多节点保证数据的完整性以及高可用。集群机制，我们已经比较熟悉了，无非是在对各机器上重复安装，在对应的配置文件上写上节点的名字，ip和集群名字之类的，它们最终的机制也大同小异，自己选举一个主节点，奔溃了就自发再选出一个。

本文主要介绍RMQ的使用，因此使用的是单节点【集群大多也是配置问题，其它的操作如果不涉及多节点问题的，有不会有太大的差异】

### 4. java代码操作

官网上已经提供了各种不同语言的使用方式，我们先学习一下较为纯粹的java代码需要如何连接并操作，这一节过去后，再介绍springBoot如何简化操作。

首先创建一个maven项目，引入`amqp-client`依赖，我当前的最新版本是`5.12.0`。对应的各种操作方式可以在官方的[教程](https://www.rabbitmq.com/getstarted.html)中学习。

最简单的工作，就是先建立一个连接，然后再创造消息的生产者和消费者的类，最后尝试着向队列添加和消费消息。

连接的建立，自然是一个工厂类配置指定的属性，再创建出我们需要的连接，原本这些属性都是可以直接写在java代码中的，但是为了更有条理，我们选择使用配置文件指明具体的信息，

在`resources`目录下随便建立一个属性文件，这里弄一个rabbbitmq.properties

```properties
rabbitmq.localhost=主机ip
rabbitmq.port=5672
rabbitmq.vhost=虚拟主机名
rabbitmq.username=用户名
rabbitmq.password=密码
```

另外建立一个负责连接创建和断开的类RabbitConnection.java

```java
//工厂是可以常驻的
private static ConnectionFactory connectionFactory=new ConnectionFactory();
public static Connection createConnect(){//用于创建连接
    try {
        ResourceBundle resource = ResourceBundle.getBundle("rabbitmq");//读取我们的属性文件
        connectionFactory.setHost(resource.getString("rabbitmq.localhost"));
        connectionFactory.setPort(Integer.parseInt(resource.getString("rabbitmq.port")));
        connectionFactory.setVirtualHost(resource.getString("rabbitmq.vhost"));
        connectionFactory.setUsername(resource.getString("rabbitmq.username"));
        connectionFactory.setPassword(resource.getString("rabbitmq.password"));
        return connectionFactory.newConnection();
    }catch (Exception e){ e.printStackTrace(); }
    return null;
}

//负责断开连接
public static void closeChnnelAndconnection(Channel channel,Connection connection){
    try {
        if(channel!=null) channel.close();
        if(connection!=null) connection.close();
    } catch (IOException e) { e.printStackTrace(); }
    catch (TimeoutException e) { e.printStackTrace();}
}
```

为了方便测试，我们还是在test目录下写测试类，【因此需要添加junit依赖，不过正常idea的maven项目都有】

#### 4.1 简单发送和获取消息

- 消息生产者

  ```java
  //发送消息的方法
  @Test
  public void sendMessage(){
      Connection connection=RabbitConnection.createConnect();
      try {
          Channel channel=connection.createChannel();//创建通道，主要利用这个通道来进行操作
          //下面对应的参数分别的含义是
          /*
                  queue:指定队列的名字
                  durable:是否持久化，如果true，那么我们的结果都将存储到磁盘中，否则，重启后便消失
                  		但即使持久化，此时也只是针对队列本身，而包括内部的消息
                  exclusive:是否独占队列，如果怕其它用户影响，可以独占，此时只能由当前的这个连接去操作
                  autoDelete:队列内的消息用完了，那么这个空队列要不要删除
                  arguments:额外的参数，是一个map类型的
               */
          //随便写个first的队列名
          channel.queueDeclare("first",true,false,false,null);
          //通道声明一个队列，表明当前通道对该队列的操作的基本设置，不代表通道只能使用这个队列，
          // 因此，一个通道可以声明多个队列
          //即使我们现在没有添加过first队列，运行后也会自动创建的
  
          //这是实际进行消息发布的方法
          channel.basicPublish("","first",MessageProperties.PERSISTENT_TEXT_PLAIN,
                               								"随便写点".getBytes());
          /*
                  上述的参数，对应如下
                  exchange:指定交换机，但我们现在还不需要
                  routingKey:指定队列名，也不一定就得是上面声明的那个队列
                  props:其它关于消息的设置,没有可以设置为null,此时我们的设置是要求发送的信息被持久化
                  body:最后是消息内容，且类型为byte[]
           */
          RabbitConnection.closeChnnelAndconnection(channel,connection);//关闭
      } catch (IOException e) {
          e.printStackTrace();
      }
  }
  ```

- 消息消费者

  消费者一般时常驻的，即不需要每次都断开连接，但是在测试类中，Junit时不支持多线程的，如果我们试图打印相关的信息，就导致需要额外一条线程进行工作，导致无法打印成功。如果每次都是最后关闭连接也还行，测试类也能打印，但可能只能打印一条记录。

  因此，我们需要在正常的java目录中，创建响应的类，并在内部使用main方法实现（方便测试，不然得整体编译运行）

  ```java
  public class ConsumerMain {
      public static void main(String[] args){
          Connection connection=RabbitConnection.createConnect();
          try {
              Channel channel=connection.createChannel();
              //对应的队列声明设置需要和我们生产者对该队列的设置相同，否则它无法确定是哪个队列
              channel.queueDeclare("first",true,false,
                      false,null);
              //下面的方法就是进行消息消费的
              /*
                  对应的参数依次为，
                  queue:指明对应的队列
                  autoAck:是否要自动确认，若是true,当提取消息时，该消息传送到套接字缓冲区，队列就认为						消息成功发送给消费者了
                          ，此时队列就会删除它。如果设为false,则消费者会发送会确认消息，队列才会删							除，否则消息将处于Unacked状态。
                  callback:回调函数，类型是个Consumer.
               */
              channel.basicConsume("first",true,new DefaultConsumer(channel){
                  //用一个匿名内部类作为参数
                  //其中body就是我们获取到的消息，这里只是简单打印出来
                  @Override
                  public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                      System.out.println(new String(body)+"\n---------");
                  }
              });
              RabbitConnection.closeChnnelAndconnection(channel,connection);
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
  }
  ```

#### 4.2 多消费者

![示例图片](https://www.rabbitmq.com/img/tutorials/exchanges.png)

这属于官网的Work Queues情况，就是一个队列被多个消费者使用，我们在上述的代码基础，多运行几个消费者的main方法就是了。

默认下，如果autoack设为true，rabbitmq将采用轮询的方式向这些消费者依此传递消息，导致每个消费者分到的消息基本上相同的，这是因为消息自动确认，导致系统认为所有的消费者都能以同样的速度快速执行，就平均分配了。如果不同消费者的性能不同，这一方案显然是不可行的。

于是，我们如果需要考虑性能，而能者多劳的话，就需要设置autoack为false.

先写一个消费者的模板类

```java
public class ConsumerExample {
    @SneakyThrows//使用lombok依赖的注解代替实现异常处理,主要是完成try catch操作
    public void example(final String consumer_name,final int sleep_time){
        Connection connection= RabbitConnection.createConnect();
        final Channel channel=connection.createChannel();
        channel.queueDeclare("first",true,false,false,null);
        channel.basicQos(1);//要求一个通道一次只能处理一条消息
        channel.basicConsume("first",false,new DefaultConsumer(channel){
            @SneakyThrows
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                System.out.println(consumer_name+"---"+
                                   new String(body)+"\n---------");//打印每条消息的结果
                //进行手动确认，如果没有这个语句，前面的消息则会一直处于为为确认状态
                channel.basicAck(envelope.getDeliveryTag(),false);
                /*
                    上述的参数对应如下，
                    deliveryTag:指明消息标识，以确认具体是哪个消息
                    multiple:是否允许依此处理多条消息的确认
                 */
                Thread.sleep(sleep_time);//使用休眠，表现出工作性能的差异
            }
        });
        //RabbitConnection.closeChnnelAndconnection(channel,connection);
    }
}
```

我们设置两个消费者即可，

```java
public class Consumer_1 {
    public static void main(String[] args){
        new ConsumerExample().example("consumer-1",5000);}}

public class Consumer_2 {
    public static void main(String[] args){
        new ConsumerExample().example("consumer-2",1000);}}
```

最后，给一个消息提供者，依旧使用测试类编写，

```java
public class ProviderMulti {
    @SneakyThrows
    @Test
    public void sendMessage(){
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();
        channel.queueDeclare("first",true,false,
                false,null);
        for (int i=0;i<20;i++){//循环产生消息
            channel.basicPublish("","first",
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    new String("随便写点"+i).getBytes());
            Thread.sleep(1500);//隔1.5秒发送一条
        }
        RabbitConnection.closeChnnelAndconnection(channel,connection);
    }
}
```

#### 4.3 多队列

- 订阅/发布

  首先写一个消息提供者的类

  ```java
  @SneakyThrows
  @Test
  public void sendmessage(){
      Connection connection= RabbitConnection.createConnect();
      Channel channel=connection.createChannel();
  
      //这里指明对应的一个交换机，如果不存在，他会自动创建一个
      //后面的fanout是指明交换机的类型，前面就提及过，交换机有着不同的类型
      /*
              direct:最简单也是默认的，在传输消息时，我们之前是以routingKey参数指定队列名，
              	   但实际这个参数没这么简单，在队列绑定时，它可以用于作为绑定键，以后的匹配可以用到
              topic:用来匹配多个队列，且队列名符合`x.y`的格式，即内部用`.`分隔开,
                    关键在于routingKey此时就变成已成匹配模式，`*`匹配任意一个单词，`#`匹配0个或多个
                    如`#.suibian.*.*`
              fanout:相当于广播，直接将消息发送给绑定的所有队列,此时routingKey是无用的
              headers:类似topic的匹配方式，只是这里的匹配是消息头,在通道声明时，有一个arguments的参					  数，那里可以用map类型指定各种变量和对应的值，就是header,
                      而在发送消息时，另一个方法
                      basicPublish(String exchange, String routingKey, boolean 										 mandatory,BasicProperties props, byte[] body)
                      其中的BasicProperties就对应着header,
                      于是，这就要求消息的头与对应的队列的arguments指定的头是相同的才行
           */
      //此外，rabbitmq本身就已经为我们的虚拟主机创建了对应的fanout类型交换机，但我们这里就任性地创建一个
      channel.exchangeDeclare("newEx","fanout");
      String queueName = channel.queueDeclare().getQueue();//这是一个临时队列
      // 当完成任务后，就自动删除，也就是autoDelete为true
      System.out.println(queueName);//随机生成一个队列，
      // 往这里输入的消息，之后其它绑定到该交换机的队列均会收到消息
      channel.queueBind(queueName,"newEx","");
      channel.basicPublish("newEx","",null,"一条发布的消息".getBytes());
      RabbitConnection.closeChnnelAndconnection(channel,connection);
  }
  ```

  写一个消费者的模板类

  ```java
  public class ConsumerMulQueExample {
      @SneakyThrows
      public void example(final String consumer_name){
          Connection connection= RabbitConnection.createConnect();
          Channel channel=connection.createChannel();
  
          //这里将绑定指定的交换机，其实这里也可以继续使用												  channel.exchangeDeclare("newEx","fanout");
          //这个方法则侧重于多个交换机的场景，
          /*
              destination:指定消息目的地的交换机，就是我们此时创建的
              source:就是消息来源对应的交换机，此时也就是目的地自己
              routingKey:此时还是没用的参数
           */
          channel.exchangeBind("newEx","newEx","");
          //channel.exchangeDeclare("newEx","fanout");
          String queueName = channel.queueDeclare().getQueue();
          System.out.println(queueName);//不同消费者都拥有一个自己独特的队列，
          // 但每个队列都会收到相同的消息，供对应的消费者使用
          //如果不使用这种随机队列的方式，读者则需要额外为不同消费者创建对应不同的队列
          channel.queueBind(queueName,"newEx","");
  
          //消费也是老样子，
          //读者可能会说了，这样子好像没什么区别，就加了个交换机，还没什么用
          //实际上，这一操作，官网上称之为 订阅/发布 模式，
          // 我们虽然只发送了一条消息，都是如果多个消费者建立了此类连接，这一条消息可以被多个消费者消费
          channel.basicConsume(queueName,true,new DefaultConsumer(channel){
              @Override
              public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                  System.out.println(consumer_name+new String(body)+"\n---------");
              }
          });
          //RabbitConnection.closeChnnelAndconnection(channel,connection);
      }
  }
  ```

  对应的两个实际的消费者类【当然可以写好几个】

  ```java
  public class ConsumerMulQue_1 {
      public static void main(String[] args){
          new ConsumerMulQueExample().example("consumerMulQue_1");}}
  public class ConsumerMulQue_2 {
      public static void main(String[] args){
          new ConsumerMulQueExample().example("consumerMulQue_2");}}
  ```

> 前面介绍了交换机的不同类型，其中也介绍了对应的direct和topic类型，下面就简要的说明一下这两种类型的匹配如何使用

- Routing

  ![示例图片](https://www.rabbitmq.com/img/tutorials/direct-exchange.png)

  这里要求对应的交换机时direct类型。

  先写消息生产者的类

  ```java
  @SneakyThrows
  @Test
  public void sendMessage(){//生产者将消息发送给指定的交换机，并附带对应的路由值
      //之后到哪个队列，则交由消费者决定
      Connection connection= RabbitConnection.createConnect();
      Channel channel=connection.createChannel();
      channel.exchangeDeclare("Ex_direct","direct");
      String[] routingkey={"A_Key","B_Key"};//这里我们随便定义一个键值
      Arrays.asList(routingkey).forEach(s -> {//数组转化为队列，lambda表达式遍历
          try { channel.basicPublish("Ex_direct",s,null,
                                     new String("指定了一个键值 "+s).getBytes());
              } catch (IOException e) { e.printStackTrace(); } });
      RabbitConnection.closeChnnelAndconnection(channel,connection);
  }
  ```

  消费者的模板类

  ```java
  public class ConsumerRoutingExample {
      @SneakyThrows
      public void example(final String consumer_name, String[] routingkeys) {
          Connection connection= RabbitConnection.createConnect();
          Channel channel=connection.createChannel();
          //这里我们由恢复使用声明的方式绑定，无它，写的内容少
          channel.exchangeDeclare("Ex_direct","direct");
          String queueName = channel.queueDeclare().getQueue();
          //可以绑定多个路由值，当交换机内存在对应路由值的消息，则将纳入到这个队列中
          Arrays.asList(routingkeys).forEach( s -> {
              try { channel.queueBind(queueName,"Ex_direct",s); }
              catch (IOException e) { e.printStackTrace(); }});
          channel.basicConsume(queueName,true,new DefaultConsumer(channel){
              @SneakyThrows
              @Override
              public void handleDelivery(String consumerTag, Envelope envelope, 											AMQP.BasicProperties properties, byte[] body) {
                  System.out.println(consumer_name+new String(body));}});
  	}
  }
  ```

  消费者

  ```java
  public class ConsumerRouting_1 {
      public static void main(String[] args){
          new ConsumerRoutingExample().example("consumer_1",
                  new String[]{"A_Key","B_Key"});
      }
  }
  public class ConsumerRouting_2 {
      public static void main(String[] args){
          new ConsumerRoutingExample().example("consumer_2",
                  new String[]{"B_Key"});
      }
  }
  ```

- Topic

  ![topic示例图片](https://www.rabbitmq.com/img/tutorials/python-five.png)
  
  前面也提到过了，topic同样时一种匹配机制，只是可以使用通配符，前提是单词之间以`.`分隔。
  
  `*` 匹配1个单词。`#`可以匹配0或多个单词。
  
  而具体的代码实现与之前的许多模式类似，其中消息生产者只需要做些简单的修改
  
  ```java
  channel.exchangeDeclare("Ex_topic","topic");//随便写个交换机名
  String[] routekeys={"Atopic.Btopic","Atopic.Btopic.Ctopic"};//随便写
  //同样这里只需要发送到交换机即可
  Arrays.asList(routekeys).forEach(s-> {
      try { channel.basicPublish("Ex_topic",s,null,
                                 new String("使用了topic--"+s).getBytes());}
      catch (IOException e) { e.printStackTrace(); } });
  
  ```
  
  而消费者模板的代码也需要修改为，
  
  ```java
  public class ConsumerTopicExample {
      @SneakyThrows
      public void example(final String consumer_name,String[] routes){
          Connection connection= RabbitConnection.createConnect();
          Channel channel=connection.createChannel();
          channel.exchangeDeclare("Ex_topic","topic");
          String queueName=channel.queueDeclare().getQueue();
          Arrays.asList(routes).forEach(s -> {
          	try { channel.queueBind(queueName,"Ex_topic",s); }
          	catch (IOException e) { e.printStackTrace(); } });
      	channel.basicConsume(queueName,true,new DefaultConsumer(channel){
          	@Override
          	public void handleDelivery(String consumerTag, Envelope envelope, 							AMQP.BasicProperties properties, byte[] body) throws IOException {
                  System.out.println(consumer_name+new String(body));}});
      }
  }
  ```
  
  消费者，
  
  ```java
  public class ConsumerTopic_1 {
      public static void main(String[] args){
          new ConsumerTopicExample().example("consumerTopic_1",new String[]																				{"Atopic.*"});}}
  public class ConsumerTopic_2 {
      public static void main(String[] args){
          new ConsumerTopicExample().example("consumerTopic_2",new String[]{"*.#"});}}
  ```

#### 4.4 远程调用

![示例图片](https://www.rabbitmq.com/img/tutorials/python-six.png)

从图中就可以发现，这已经不是简单的消息存放的操作了，应该说，我们之前的操作都是纯粹的消息中间件的工作。

而这里则是利用消息中间件的作用完成工作，由于一些功能是服务端特有的，因此客户端需要发送消息请求服务端完成某些工作以提供响应的信息。那么正常情况下，客户端肯定需要和服务端中的相关服务建立关系，以发送任务并获取结果。这样的话，耦合度太高。【当然了，如果解耦了也没什么好处就别用了，一切以实际场景为主】

于是，我们需要创建一个请求队列，客户端们的请求消息将纳入其中，服务端也从这里提取任务。而每个客户端为了获取结果则需要为自己创建一个唯一的回调队列，用于获取返回的消息。

官网的教程页面内，对相应的客户端和服务端都有对应的代码示例文件，例如对应的客户端示例文件[RPCClient.java ](https://github.com/rabbitmq/rabbitmq-tutorials/blob/08d574812fa0f2c5b84a8562bc13df123f29c458/java/RPCClient.java#L12)，读者可以直接看看官方的示例代码。我们下文简化了许多。

那么我们的代码，先定义一个服务端的类，其中需要指明请求队列，并定义用于提供服务的方法，

```java
//这里是我们将要使用的请求队列的名字
    private static final String RPC_QUEUE_NAME = "rpc_queue";
    //首先定义一个用于提供服务的方法，当然可以多定义几个，只要客户端传送的消息能够指定把不同的服务即可
    private static String responseCall(String message){
        String reslut="你已经来过服务器了"+message+"!!!";
        return reslut;
    }
    @SneakyThrows
    public static void main(String[] args){
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();
        channel.queueDeclare(RPC_QUEUE_NAME,false,false,false,null);
        //channel.queuePurge(RPC_QUEUE_NAME); // 如果该请求队列是已有的，可能内部有其它剩余的消息，可以使用该方法清理
        channel.basicQos(1);//指明服务器每次能传递的消息最大数量，如果是0反而代表没有限制
        Object monitor=new Object();//用与后面的同步锁
        //之前我们进行消费的 basicConsume都是使用的DefaultConsumer,这里也可以使用
        //但是官方的示例代码使用了 DeliverCallback ，我们也尝试一下
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {//这是用lambda的方式实现一个接口，接口必须只有一个方法
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())//指明该请求对应的唯一值，方便客户端判断结果对应的是哪个请求
                    .build();
            String response = "";
            try {
                String message = new String(delivery.getBody(), "UTF-8");//获取请求内容
                response += responseCall(message);//完成服务
            } catch (RuntimeException e) { System.out.println(" [.] " + e.toString());
            } finally {
                //接下来就是将结果发送到指定的队列中
                /*
                    对应的参数是，
                    exchange:这里我们没有设置交换机，
                    routingKey:这里的getReplyTo()返回客户端传送过来的回调队列的名字
                    BasicProperties:这里就是指明结果的各种信息
                    body[]:消息主体
                 */
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                //手动发送确认，因为我们的请求队列没有设置为自动确认。
                /*
                    deliveryTag；对应的是消息是否确实获取到
                    mutiple:对应的是是否用于确认多条消息的确认，这里的false表明一条一条地确认
                 */
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                // RabbitMq的消费者工作线程 通知  RPC 服务器 拥有者的线程
                synchronized (monitor) { monitor.notify(); }
            }
        };
        //消费请求队列中的消息
        //最后的参数，使用了lambda表达式没有做具体的实现，只是对应的位置需要有一个CancelCallback的对象
        //在下面的 发布者确认模式 中我们还会了解到一个ConfirmCallback
        channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> { }));
        // 等待并时刻准备着消费客户端发来的消息.
        while (true) {
            synchronized (monitor) {
                try { monitor.wait(); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
    }
```

而客户端的类，一方面需要自己创建一个回调队列，另一方面需要与请求队列建立联系，并且发送请求的相关信息，否则服务端怎么知道结果和哪个客户端对应，而客户端有需要知道结果是对应的哪个请求的

```java
//指明请求队列的名字
    private String requestQueueName = "rpc_queue";
    @Test
    @SneakyThrows
    public void call(){//实际使用时，这个是作为主函数的调用方法，内部包含消息的参数，这里是方便测试
        String message="请求服务消息";
        String result=null;
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();
        final String corrId = UUID.randomUUID().toString();//为消息准备一个唯一值，随便那什么方法生成，只要保证唯一就行
        String replyQueueName = channel.queueDeclare().getQueue();//用于作为回调队列的名字

        /*
        属性设置的常用内容：
            delivery_mode（投递模式）：将消息标记为持久的（值为2）或暂存的（除了2之外的其他任何值）。第二篇教程里接触过这个属性，记得吧？
            content_type（内容类型）:用来描述编码的 mime-type。例如在实际使用中常常使用 application/json 来描述 JOSN 编码类型。
            reply_to（回复目标）：通常用来命名回调队列。
            correlation_id（关联标识）：用来将RPC的响应和请求关联起来。
        */
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)//指定消息的唯一值
                .replyTo(replyQueueName)//指定结果进入的回调队列
                .build();
        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));
        //用于接收结果,容量是一个
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        //消费回调队列中的结果
        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8")); }}, consumerTag -> {});
        result = response.take();
        channel.basicCancel(ctag);//取消队列的订阅关系
        RabbitConnection.closeChnnelAndconnection(channel,connection);
        System.out.println(result);
    }
```

#### 4.5 发布者确认

这一模式是用于保证消息都确实被消费者获取，前面我们已经使用过手动确认，要求服务器返回相应的回应。但是我们当然还是希望有更方便的方式、更丰富的功能完成相关的操作。

这个模式，则主要是要求当前的通道转变为这样的发布者确认模式，当发布了消息后，可以通过指定的方法等待服务器的回应，回应可以是成功，也可能是拒绝接收消息，当指定的时间内未收到回应，则认为失败，将重新发布消息。具体的操作，可以是每条消息都等待回应，也可以是一批消息，还可以是异步处理。

同样的，可以看一下官方的示例代码，下面，我们将基于官方的代码做一些简化，

```java
public class Confirms {
    static final int MESSAGE_COUNT = 50_000;
    private static Connection connection=RabbitConnection.createConnect();
    @Test
    public  void publishMessagesIndividually() throws Exception {//仅发布一条消息
        Channel channel = connection.createChannel();
        String queue = UUID.randomUUID().toString();
        channel.queueDeclare(queue, false, false, true, null);
        channel.confirmSelect();//声明为发布者确认模式
        long start = System.nanoTime();//记录时间而已
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String body = String.valueOf(i);
            channel.basicPublish("", queue, null, body.getBytes());

            //等待直到自最后一次调用以来发布的所有消息已经被回应，这里包括成功和失败（当然不是不回应的失败）;
            // //如果在给定的时间内为回应，则抛异常
            channel.waitForConfirmsOrDie(5_000);
        }
        long end = System.nanoTime();//记录一下整个的时间而已
        System.out.format("Published %,d messages individually in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
    }
    @Test
    public void publishMessagesInBatch() throws Exception {//发布一个批次的消息
        Channel channel = connection.createChannel();
        String queue = UUID.randomUUID().toString();
        channel.queueDeclare(queue, false, false, true, null);
        channel.confirmSelect();
        int batchSize = 100;//表明一个批次的消息量为100
        int outstandingMessageCount = 0;
        long start = System.nanoTime();
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String body = String.valueOf(i);
            channel.basicPublish("", queue, null, body.getBytes());
            outstandingMessageCount++;
            if (outstandingMessageCount == batchSize) {//达到一个批次的量了
                channel.waitForConfirmsOrDie(5_000);//等待之前的消息有回应
                outstandingMessageCount = 0;
            }
        }
        if (outstandingMessageCount > 0) {//剩余的不够一批次
            channel.waitForConfirmsOrDie(5_000);
        }
        long end = System.nanoTime();
        System.out.format("Published %,d messages in batch in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
    }
    @Test
    public void handlePublishConfirmsAsynchronously() throws Exception {//异步
        Channel channel = connection.createChannel();
        String queue = UUID.randomUUID().toString();
        channel.queueDeclare(queue, false, false, true, null);
        channel.confirmSelect();
        ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();//这里new的是一个跳表，不知道的读者可以了解一下（当然与下面的代码没有具体关系）
        ConfirmCallback cleanOutstandingConfirms = (sequenceNumber, multiple) -> {
            if (multiple) {//这里表示可以删除多个
                //headMap是指返回表中对应键值小于sequenceNumber的记录，后面的参数为 inclusive，如果为true，则表示可以是小于等于。
                ConcurrentNavigableMap<Long, String> confirmed = outstandingConfirms.headMap(sequenceNumber, true);
                confirmed.clear();
            } else { //这里表示只能删除一个
                outstandingConfirms.remove(sequenceNumber);
            }
        };

        /*
            两个参数均是ConfirmCallback类型，
            前者为 ackCallback：意味成功，就直接删除原有的消息了
            后者为 nackCallback：意味失败，则做一些表示，如下面输出哪个消息失败了，再执行删除
         */
        //到这里，基本为后面的具体操作，做了规划
        channel.addConfirmListener(cleanOutstandingConfirms, (sequenceNumber, multiple) -> {
            String body = outstandingConfirms.get(sequenceNumber);
            System.err.format(
                    "Message with body %s has been nack-ed. Sequence number: %d, multiple: %b%n",
                    body, sequenceNumber, multiple
            );
            cleanOutstandingConfirms.handle(sequenceNumber, multiple);
        });
        long start = System.nanoTime();
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String body = String.valueOf(i);
            outstandingConfirms.put(channel.getNextPublishSeqNo(), body);
            channel.basicPublish("", queue, null, body.getBytes());
        }

        //如果60秒后，消息没有全部得到回应，就抛异常
        if (!waitUntil(Duration.ofSeconds(60), () -> outstandingConfirms.isEmpty())) {
            throw new IllegalStateException("All messages could not be confirmed in 60 seconds");
        }
        long end = System.nanoTime();
        System.out.format("Published %,d messages and handled confirms asynchronously in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
    }

    public boolean waitUntil(Duration timeout, BooleanSupplier condition) throws InterruptedException {
        int waited = 0;
        //不到60秒，而且消息还有剩余（只要有剩余，就说明消息没有全部得到回应），
        while (!condition.getAsBoolean() && waited < timeout.toMillis()) {//这里对应的是60秒的毫秒数
            Thread.sleep(100L);// 避免频繁循环，本来异步操作就是想慢慢等的，不急的
            waited = +100;//当等待的时间超过了60秒，就跳出循环
        }
        return condition.getAsBoolean();
    }
}
```

### 5. SpringBoot下的使用

使用了SpringBoot后，上述的操作都会变得非常简单，我们这里仅列出消息中间件的操作，其它模式，读者基本可以自己摸索出来。

需要的依赖有`spring-boot-starter-amqp`, `spring-boot-starter-web`, 另外不是必须的，只是我们使用测试，别忘了junit。而springboot也有自己对应的测试依赖，`spring-boot-starter-test`, `spring-rabbit-test`。

- Hello World

  生产者

  ```java
  @SpringBootTest(classes = DemoApplication.class)
  @RunWith(SpringRunner.class)
  public class ProviderHello {
      @Autowired
      private RabbitTemplate rabbitTemplate;
      @Test
      public void sendMessage(){
          //hello就是指定的队列，后面的参数就是消息内容，这里不用我们转化为byte[]
          rabbitTemplate.convertAndSend("hello","一条消息");
      }
  }
  ```

  ```java
  @SpringBootApplication
  public class DemoApplication {//这是创建项目自动生成的，名字随意，主要作为项目的入口
      public static void main(String[] args) {
          SpringApplication.run(DemoApplication.class, args);
      }
  }
  ```

  消费者

  ```java
  //单纯有前面的生产者，运行时没有效果的，只有存在消费者，才能真正创建对应的队列
  @Component
  @RabbitListener(queuesToDeclare = @Queue("hello"，,durable = "false"))//表示这是一个消费者，并指明消费的队列名
  //这里的@Queue,内部可以指明队列的各种属性，更多的可以查看源码，默认属性是true
  //并且，这个注解也是可以放在方法上的，因此可以在一个类中配置针对多个队列的消费
  public class ConsumerHello {
      @RabbitHandler
      public void consumer(String message){//这里它自动就从队列中获取消息内容
          System.out.println("消费者 "+message);
      }
  }
  ```

- Work Queues

  生产者

  ```java
  @SpringBootTest(classes = DemoApplication.class)
  @RunWith(SpringRunner.class)
  public class ProviderWork {
      @Autowired
      RabbitTemplate rabbitTemplate;
      @Test
      public void sendMessage(){
          rabbitTemplate.convertAndSend("work","work模式内容");
      }
  }
  ```

  消费者

  ```java
  @Component
  public class ConsumerWork {
      @RabbitListener(queuesToDeclare = @Queue(value="work",durable = "false"))
      public void consume(String message){
          System.out.println("work消费者"+message);
      }
  }
  ```

  











