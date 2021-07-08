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
```

而交换机（Exchage）和队列（queue）的创建则可以在对应的浏览器页面上操作。其中交换机有多种类型，具体的不同到时候再说。

此外，队列可以有多个消息生产者，也可以有多个消息消费者。

而这类需要面对较大应用场景的软件，为了保证数据的安全性，它也具有集群机制，可通过多节点保证数据的完整性以及高可用。集群机制，我们已经比较熟悉了，无非是在对各机器上重复安装，在对应的配置文件上写上节点的名字，ip和集群名字之类的，它们最终的机制也大同小异，自己选举一个主节点，奔溃了就自发再选出一个。

本文主要介绍RMQ的使用，因此使用的是单节点【集群大多也是配置问题，其它的操作如果不涉及多节点问题的，有不会有太大的差异】

### 4. java代码操作

官网上已经提供了各种不同语言的使用方式，我们先学习一下较为纯粹的java代码需要如何连接并操作，这一节过去后，再介绍springBoot如何简化操作。









