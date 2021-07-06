## RabbitMQ介绍

>  作为消息队列，也可以称为消息中间件，主要是以异步的方式进行消息传递，一方作为生产者向队列填充消息，另一方从队列中不断获取消息，就是这么简单的功能。

### 1. 安装

具体的安装方式在官网对不同的系统都有介绍，我这个时间段最新的是3.8.19。我们就以最新的版本为例，系统为Centos8。

首先安装Erlang，因为RabbitMQ就是这个语言写的，可以使用rabbitmq提供的一个erlang的[安装包]([rabbitmq/erlang-rpm: Latest Erlang/OTP releases packaged as a zero dependency RPM, just enough for running RabbitMQ (github.com)](https://github.com/rabbitmq/erlang-rpm))，这是在github上的项目，只包含了rabbitmq所需的包，相比大多数读者不太会过多的使用Erlang，推荐使用这个，下载一个最新的就行，我这里最新的版本是[24.0.3](https://github.com/rabbitmq/erlang-rpm/releases/tag/v24.0.3)。放到系统中，yum安装就好。

然后是下载rabbitmq的[rpm文件](https://github.com/rabbitmq/rabbitmq-server/releases/download/v3.8.19/rabbitmq-server-3.8.19-1.el8.noarch.rpm)，用yum安装，在这之前，先检查一下自己的centos系统中是否有socat 、logrotate，没有就安装。

这里组件尽量都尝试在linux上安装，最好别在windows上装，macOS安装也没什么问题。

### 2. 配置

以前的版本，可能会提供一个类似rabbitmq.config.example的文件方便我们修改为rabbitmq.config，但是当前而言，没有example文件，对应的文件也变成了rabbitmq.conf。另外还配备了文件advanced.config，同样安装之后并不会为我们提供示例文件，但可以在github项目的[docs]([rabbitmq-server/deps/rabbit/docs at v3.8.x · rabbitmq/rabbitmq-server (github.com)](https://github.com/rabbitmq/rabbitmq-server/tree/v3.8.x/deps/rabbit/docs))中找到。



