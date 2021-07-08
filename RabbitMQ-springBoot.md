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
        channel.queueDeclare("first",true,false,
                false,null);
        channel.basicQos(1);//要求一个通道依此只能处理一条消息
        channel.basicConsume("first",false,new DefaultConsumer(channel){
            @SneakyThrows
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                System.out.println(consumer_name+"---"+
                                   new String(body)+"\n---------");
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
            Thread.sleep(1200);
        }
        RabbitConnection.closeChnnelAndconnection(channel,connection);
    }
}
```













