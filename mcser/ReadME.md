这一目录下的文章主要是针对微服务的一些组件介绍。

主要针对spring cloud alibaba的技术组件。



![](https://mudongjing.github.io/gallery/cloud/alibaba/frame/frame.jpg)

> 微服务的产生的，就像人类社会出现分工一样，目的在于让专业的人更专业，让工作责任更明确。
>
> > 谈及微服务，我们总会和分布式相关联，可以说是分布式让专业更专业，而组件分离的模块化，则是让工作责任更明确。

如上图中，一个基本的微服务体系大致的工作流程是，

> - 用户提交一个请求，由网关根据规则判断这一请求是否合法，并进行转发，
>   - 之所以要转发，因为我们的服务很多，用户却只记得一个网址，因此在该网址发出的请求，到了后端必须转发到对应的API来负责相应的处理。
> - 由sentinel之类的组件负责流量控制，
>   - 网关一定程度上隔绝了一下非法的访问。但是，如果访问量过高，导致系统发生崩溃，也是一种巨大的灾难
>   - 如果访问量或连接数超过指定的阈值，我们就会做出限制，可能是拒绝，也可能只是稍微搁置该请求
> - 重头戏，服务发现，服务配置，目前我们比较喜欢使用Nacos
>   - 我们实现的一个订单业务或交易业务都属于一个服务
>   - 我们构建的所有服务，最终都是体系中的一个服务而已，需要由网关进行转发
>   - 但是，一个服务一旦专业起来就一般不是一台机器，那么需要这样一个组件记住该服务对应的机器
>     - 我们会利用Ribbon之类的组件，完成负载均衡的操作
>     - openfeign之类的就是编程时的一个简化的组件
>   - 服务发现是管理我们的API。而服务配置则基本是管理我们其它组件的配置，以后，我们可能会向这个体系中，塞入其它的组件，那么利用服务配置，我们可以存储这些组件在这里的配置信息。
> - 现在，有了Nacos完成的服务发现和服务配置，已经可以处理处理一个事务，但是，虽然可以轻松地调用多个API完成对应的工作，但分配在不同的机器上，Seata就是用来大致保证一次事务能不发生错误。
>   - 蚂蚁金服就是开发的这个用于支付宝交易的开发
>   - 而这其中，为了保证消息的一致性，中间的消息传递使用消息队列与参与的各个组件进行反复的通信。
> - 而不同的组件之间不在一个机器上，需要只用如Dubbo之类的远程调用组件进行通信。
>
> - 现在大体上的功能已经完成，为了时刻监控我们的各项服务，可以添加一个如Skywalking的链路追踪组件，负责查看系统中各项服务中的运行情况。

------------------------------

下面我们将使用spring-cloud-alibaba构建一个项目，

读者可以在[版本说明](https://github.com/alibaba/spring-cloud-alibaba/wiki/版本说明)中，查看当前较新的版本依赖关系 

> | Spring Cloud Alibaba Version                              | Sentinel Version | Nacos Version | RocketMQ Version | Dubbo Version | Seata Version |
> | --------------------------------------------------------- | ---------------- | ------------- | ---------------- | ------------- | ------------- |
> | 2.2.6.RELEASE                                             | 1.8.1            | 1.4.2         | 4.4.0            | 2.7.8         | 1.3.0         |
> | 2021.1 or 2.2.5.RELEASE or 2.1.4.RELEASE or 2.0.4.RELEASE | 1.8.0            | 1.4.1         | 4.4.0            | 2.7.8         | 1.3.0         |
> | 2.2.3.RELEASE or 2.1.3.RELEASE or 2.0.3.RELEASE           | 1.8.0            | 1.3.3         | 4.4.0            | 2.7.8         | 1.3.0         |
> | 2.2.1.RELEASE or 2.1.2.RELEASE or 2.0.2.RELEASE           | 1.7.1            | 1.2.1         | 4.4.0            | 2.7.6         | 1.2.0         |
> | 2.2.0.RELEASE                                             | 1.7.1            | 1.1.4         | 4.4.0            | 2.7.4.1       | 1.0.0         |
> | 2.1.1.RELEASE or 2.0.1.RELEASE or 1.5.1.RELEASE           | 1.7.0            | 1.1.4         | 4.4.0            | 2.7.3         | 0.9.0         |
> | 2.1.0.RELEASE or 2.0.0.RELEASE or 1.5.0.RELEASE           | 1.6.3            | 1.1.1         | 4.4.0            | 2.7.3         | 0.7.1         |
>
> 毕业版本依赖关系(推荐使用)
>
> | Spring Cloud Version        | Spring Cloud Alibaba Version      | Spring Boot Version |
> | --------------------------- | --------------------------------- | ------------------- |
> | Spring Cloud 2020.0.1       | 2021.1                            | 2.4.2               |
> | Spring Cloud Hoxton.SR9     | 2.2.6.RELEASE                     | 2.3.2.RELEASE       |
> | Spring Cloud Greenwich.SR6  | 2.1.4.RELEASE                     | 2.1.13.RELEASE      |
> | Spring Cloud Hoxton.SR3     | 2.2.1.RELEASE                     | 2.2.5.RELEASE       |
> | Spring Cloud Hoxton.RELEASE | 2.2.0.RELEASE                     | 2.2.X.RELEASE       |
> | Spring Cloud Greenwich      | 2.1.2.RELEASE                     | 2.1.X.RELEASE       |
> | Spring Cloud Finchley       | 2.0.4.RELEASE(停止维护，建议升级) | 2.0.X.RELEASE       |
> | Spring Cloud Edgware        | 1.5.1.RELEASE(停止维护，建议升级) | 1.5.X.RELEASE       |

但这里我们并不打算使用最新的，而是使用如下的版本

> springBoot: 2.3.3.RELEASE
>
> spring cloud: Hoxton.SR8
>
> spring cloud alibaba: 2.2.6.RELEASE
>
> jdk: 1.8
>
> > 主要在在于spring cloud alibaba 2021.1中的nacos discovery中移除了ribbon依赖，导致以往常用的负载均衡的使用发生变化。

然后，再下载对应的组件

> nacos: [Releases · alibaba/nacos (github.com)](https://github.com/alibaba/nacos/releases)，下载1.4.2版本。
>
> sentinel: [Releases · alibaba/Sentinel (github.com)](https://github.com/alibaba/Sentinel/releases)，下载版本1.8.1。
>
> seata: [Releases · seata/seata (github.com)](https://github.com/seata/seata/releases)，下载版本1.3.0。
>
> rocketMQ: [Release Notes - Apache RocketMQ - Version 4.4.0 - Apache RocketMQ](https://rocketmq.apache.org/release_notes/release-notes-4.4.0/)
>
> dubbo: [Releases · apache/dubbo (github.com)](https://github.com/apache/dubbo/releases?after=dubbo-2.7.9)，下载2.7.8。【是源码】
>
> > dubbo的源码下载并解压后，我们在对应的目录下，运行命令
> >
> > ```bash
> > #对源码进行编译
> > mvn install -Dmaven.test.skip=true # 需要配置对应的maven环境
> > # 估计需要一二十分钟
> > # 再
> > mvn idea:idea #对应的是idea软件
> > ```
> >
> > 

我们现在建立一个alibaba的项目，内部包含各个组件的项目module,

> 使用idea，进行项目构建，
>
> 首先是利用spring initializer创建一个空项目，如果此时能指定对应的springBoot版本最好，不然就在项目构建后再pom文件中修改。
>
> 大致的内容如下
>
> > ```xml
> > <?xml version="1.0" encoding="UTF-8"?>
> > <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
> >          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
> >     <modelVersion>4.0.0</modelVersion>
> >     <parent>
> >         <groupId>org.springframework.boot</groupId>
> >         <artifactId>spring-boot-starter-parent</artifactId>
> >         <version>2.3.3.RELEASE</version>
> >         <relativePath/> 
> >     </parent>
> >     <groupId>com.example</groupId>
> >     <artifactId>demo</artifactId>
> >     <version>0.0.1-SNAPSHOT</version>
> >     <packaging>pom</packaging>
> >     <name>demo</name>
> >     <description>Demo project for Spring Boot</description>
> >     <properties>
> >         <java.version>1.8</java.version>
> >         <spring.cloud.alibaba.version>2.2.6.RELEASE</spring.cloud.alibaba.version>
> >         <spring.cloud.version>Hoxton.SR8</spring.cloud.version>
> >     </properties>
> >     <!-- 这里我们进行了版本管理，将作为父依赖，指定其内部各种组件对应的版本好-->
> >     <!--包括了spring cloud alibaba, spring cloud-->
> >     <dependencyManagement>
> >         <dependencies>
> >             <dependency>
> >                 <groupId>com.alibaba.cloud</groupId>
> >                 <artifactId>spring-cloud-alibaba-dependencies</artifactId>
> >                 <version>${spring.cloud.alibaba.version}</version>
> >                 <type>pom</type>
> >                 <scope>import</scope>
> >             </dependency>
> >             <dependency>
> >                 <groupId>org.springframework.cloud</groupId>
> >                 <artifactId>spring-cloud-dependencies</artifactId>
> >                 <version>${spring.cloud.version}</version>
> >                 <type>pom</type>
> >                 <scope>import</scope>
> >             </dependency>
> >         </dependencies>
> >     </dependencyManagement>
> >     <dependencies>
> >         <dependency>
> >             <groupId>org.springframework.boot</groupId>
> >             <artifactId>spring-boot-starter</artifactId>
> >         </dependency>
> > 
> >         <dependency>
> >             <groupId>org.springframework.boot</groupId>
> >             <artifactId>spring-boot-starter-test</artifactId>
> >             <scope>test</scope>
> >         </dependency>
> >     </dependencies>
> > 
> >     <build>
> >         <plugins>
> >             <plugin>
> >                 <groupId>org.springframework.boot</groupId>
> >                 <artifactId>spring-boot-maven-plugin</artifactId>
> >             </plugin>
> >         </plugins>
> >     </build>
> > </project>
> > ```
>
> 此后，我们可以在项目中构建module，分别负责具体任务。

下面我们简单叙述一个基本的业务系统，如何利用cloud alibaba的微服务框架组装各个模块。

> 我们这里简单涉及两个服务，一个是购买商品的订单服务，另一个是对应的库存修改服务。
>
> > 两个服务对应着不同的项目模块，利用MVC生成对应的API，用以处理对应的业务，
> >
> > > 如果订单服务量较大，我们需要几个机器共同处理订单服务，于是几个机器的服务启动时注册到nacos discovery中，我们只需要在nacos中调用对应的服务名即可调用对应的机器上的服务，
> > >
> > > > 但此时单纯地使用服务名是无法获得对应的机器，需要一个负载均衡的组件，帮助识别服务名，并将用户需求分派到各个机器上。
> > >
> > > 但是用户也不会直接使用对应的API修改订单，比如使用淘宝的网页进行购物，地址多半前面是`buy.tmall.com`之类的前缀，显然需要转发到我们实际使用的API地址，于是需要Gateway之类的网管组件负责转发，转发时，也可以使用对应的服务名进行负载均衡。
> >
> > > 如果订单量更大，导致系统无法承受，则必须由Sentinel之类的组件拒绝一部分的请求，拒绝的依据有QPS或线程数。如果不希望直接拒绝一下用户的请求，可以只用消息队列，如RocketMQ暂存一部分的请求，等进来的请求处理结束再处理队列中的请求。
>
> > 两个对应的业务完成后，订单服务执行的差不多的时候，就需要调用库存服务进行相关商品数量的修改，此时就需要只用Dubbo进行不同组件的远程调用，
> >
> > > 假设我们有两个数据库分别记录已销售的数量，另一个记录库存数量，那么就两个数据库的修改就是一个事务，
> > >
> > > > 由于两个数据库处于不同的机器上，需要远程调用来进行处理，事务的一致性和原子性就很难保证，于是就有Seata组件进行这种事务的实现。
>
> 而我们目前使用的Sentinel、Seata等组件本身也需要进行配置，有的组件没有对配置进行持久化，则需要nacos config这样的组件进行各种组件配置的存储。













