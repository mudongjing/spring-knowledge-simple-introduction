# 安装

首先，读者应该已经下载了对应的压缩文件，解压即可。运行`/bin/seata-server.sh或.bat`即可，如

```bash
sh seata-server.sh -p 端口 -h 地址
```

但我们需要稍微做一下配置，对`conf`目录下的几个文件的内容做修改

> - file.conf
>
>   内部的主要内容是存储模式和该存储模式的设置。默认是`file`，是在本机内存中存储数据。另外还有`db`和`redis`模式。如果是单机使用，自然使用file即可，当然使用数据库也挺好，如果是集群模式，则最好使用`db`模式，即使用数据库，`redis`也可以。更深入的，我们也可能需要进一步的将数据库或redis进行集群化。
>
>   文件内容结构，
>
>   ```
>   store {
>   ## store mode: file、db、redis
>     mode = "db"
>    # 指定db模式的配置
>     db {
>       ## the implement of javax.sql.DataSource, such as DruidDataSource(druid)/BasicDataSource(dbcp)/HikariDataSource(hikari) etc.
>       datasource = "druid"
>       ## mysql/oracle/postgresql/h2/oceanbase etc.
>       dbType = "mysql"
>       driverClassName = "com.mysql.jdbc.Driver"
>       url = "jdbc:mysql://127.0.0.1:3306/seata"
>       user = "mysql"
>       password = "mysql"
>       minConn = 5
>       maxConn = 30
>       globalTable = "global_table"
>       branchTable = "branch_table"
>       lockTable = "lock_table"
>       queryLimit = 100
>       maxWait = 5000
>     }
>   }
>   ```
>   
>   至于该数据库中的表如何设置，官方也有准备，可下载seata的项目源码，在`/script/server/db`中存在一个`mysql.sql`的文件，可以在读者的mysql数据库中运行，其它还支持oracle、postgresql数据库，均有对应的脚本文件。

> - registry.conf
>
>   此外我们还需要一个配置中心注册我们的seata服务，可以使用file 、nacos 、eureka、redis、zk、consul、etcd3、sofa几种方式，我们这里自然还是使用nacos进行注册，同时也建立对应的配置内容，二者是独立的，只不过我们这里都是使用的nacos罢了，
>
>   > 而配置内容将包含seata的各项设置，可以查看源码的`script/config-center/config.txt`文件。
>   >
>   > 启动后，seata就会将config.txt文件中描述的内容放入配置中心。
>
>   文件内容结构，
>
>   ```
>   registry {
>     # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
>     type = "nacos"
>     # 还可以自己指定负载均衡的策略
>     loadBalance=""
>     nacos {
>       application = "seata-server"
>       #nacos地址
>       serverAddr = "127.0.0.1:8848" 
>       group = "SEATA_GROUP"
>       namespace = ""
>       cluster = "default"
>       username = ""
>       password = ""
>     }
>   }
>         
>   config {
>     # file、nacos 、apollo、zk、consul、etcd3
>     type = "nacos"
>         
>   # 对应nacos的相关信息
>     nacos {
>       serverAddr = "127.0.0.1:8848"
>       namespace = ""
>       group = "SEATA_GROUP"
>       username = ""
>       password = ""
>     }
>   }
>   ```
>
>   有了nacos复杂管理seata，我们就不需要自己考虑seata的集群模式，可以任意添加seata，只需要向nacos注册一下即可，同时还能实现负载均衡。

完成配置文件的修改后，我们需要先把关于seata的配置填入nacos中，否则，seata启动后试图从nacos中获取对应的数据会失败。

读者应该也已经下载了seata项目的源码，我们需要做的就是把源码中的`script/config-center/config.txt`文件的内容提交给nacos，而`script/config-center/nacos`目录下有`nacos-config.py`和 `nacos-config.sh`两个文件，均可用于自动完成内容配置的任务，

> linux上或macos上一般都是可以直接运行sh脚本文件，前提是你的nacos的ip和端口都是默认的，否则需要额外做修改工作，如果windows，可以安装git客户端，在其中运行sh脚本，或是安装python，执行py脚本。

> config.txt各属性的作用可查看[附录](#seata参数配置 1.3.0版本)

> 下面我们简单介绍一下这些脚本的内容，附带使用方法。
>
> - sh
>
>   shell脚本写得比较清晰，
>
>   首先是指明我们可以使用的外部命令
>
>   ```shell
>   while getopts ":h:p:g:t:u:w:" opt
>   ```
>
>   <details>
>       <summary>do</summary>
>        &emsp;case $opt in<br>
>    &emsp; h)<br>
>    &emsp;&emsp;   host=$OPTARG<br>
>    &emsp;&emsp;   ;;<br>
>    &emsp; p)<br>
>    &emsp;&emsp;   port=$OPTARG<br>
>    &emsp;&emsp;   ;;<br>
>   &emsp;  g)<br>
>    &emsp;&emsp;   group=$OPTARG<br>
>    &emsp;&emsp;   ;;<br>
>   &emsp;  t)<br>
>     &emsp;&emsp;  tenant=$OPTARG<br>
>     &emsp;&emsp;  ;;<br>
>   &emsp;  u)<br>
>     &emsp;&emsp;  username=$OPTARG<br>
>     &emsp;&emsp;  ;;<br>
>   &emsp;  w)<br>
>     &emsp;&emsp;  password=$OPTARG<br>
>     &emsp;&emsp;  ;;<br>
>   &emsp;  ?)<br>
>    &emsp;&emsp;   echo " USAGE OPTION: $0 [-h host] [-p port] [-g group] [-t tenant] [-u username] [-w password] "<br>
>    &emsp;&emsp;   exit 1<br>
>     &emsp;&emsp;  ;;<br>
>    &emsp;&emsp; esac<br>
>   &emsp;done<br>
>      if [ -z ${host} ]; then<br>
>   &emsp;    host=localhost<br>
>   fi<br>
>   if [ -z ${port} ]; then<br>
>   &emsp;    port=8848<br>
>   fi<br>
>   if [ -z ${group} ]; then<br>
>   &emsp;    group="SEATA_GROUP"<br>
>   fi<br>
>   if [ -z ${tenant} ]; then<br>
>   &emsp;    tenant=""<br>
>   fi<br>
>   if [ -z ${username} ]; then<br>
>   &emsp;    username=""<br>
>   fi<br>
>   if [ -z ${password} ]; then<br>
>   &emsp;    password=""<br>
>   fi<br>
>   </details>
>
>   在do命令中，指出默认的nacos的ip和端口就是默认的那个值，以及组名，命名空间没有值，就是对应着public。
>
>   简单而言，使用时就是
>
>   ```bash
>   sh nacos-config.sh -h 地址 -p 端口 -g 组名 -t 命名空间 -u 用户名 -w 登录密码
>   ```
>
>   > 读者最好为seata单独创建一个命名空间，seata的配置内容还是有点恶心的，把config.txt中的内容每行都建立一个dataid。
>
>   然后拼接出nacos的地址，
>
>   ```shell
>   nacosAddr=$host:$port
>   contentType="content-type:application/json;charset=UTF-8"
>   
>   echo "set nacosAddr=$nacosAddr"
>   echo "set group=$group"
>   ```
>
>   中间一些环节可以不看，最后就是提取config.txt的内容，
>
>   ```shell
>   count=0
>   for line in $(cat $(dirname "$PWD")/config.txt | sed s/[[:space:]]//g); do
>       count=`expr $count + 1`
>   	key=${line%%=*}
>       value=${line#*=}
>   	addConfig "${key}" "${value}"
>   done
>   # addConfig是前面定义的一个函数，用来通过API进行配置内容的设置，代码中可以看出是一行一行的单独设置
>   # 另外config.txt的文件位置，也不要随便挪动，代码中就是认为在上层的目录中
>   ```
>
> - py
>
>   python写得稍微没有shell的直观，但也简洁。
>
>   开头，抱怨必须指定nacosAddr，从前面的shell脚本，可以知道就是写上`ip:端口`
>
>   ```python
>   if len(sys.argv) < 2:
>       print ('python nacos-config.py nacosAddr')
>       exit()
>   ```
>
>   省略一些内容，下面是进行内容配置和namespace
>
>   ```shell
>   for line in open('../config.txt'):# 同样读取上层目录的config.txt文件
>       pair = line.rstrip("\n").split('=')
>       if len(pair) < 2:
>           continue
>       print (line),
>       url_prefix = sys.argv[1] # 这里读取外部参数给定的nacosAddr，也提示我们必须按顺序给参数
>       conn = http.client.HTTPConnection(url_prefix)
>       if len(sys.argv) == 3:
>           namespace=sys.argv[2] #然后就是命名空间
>           url_postfix = '/nacos/v1/cs/configs?dataId={0}&group=SEATA_GROUP&content={1}&tenant={2}'.format(urllib.parse.quote(str(pair[0])),urllib.parse.quote(str(pair[1])).strip(),namespace)
>       else:
>           url_postfix = '/nacos/v1/cs/configs?dataId={}&group=SEATA_GROUP&content={}'.format(urllib.parse.quote(str(pair[0])),urllib.parse.quote(str(pair[1]))).strip()
>   ```
>
>   但似乎没有指定用户名和密码的指令，读者可以参考shell脚本中的API格式在其中的地址上添加上类似`&username=$username&password=$password`的附加参数，\$username或\$​password需要换成自己设定的变量或直接写上自己的值。

> config.txt文件中有几个是没有对应的值，导致出现几个失败的，也无需太惊讶。
>
> **其中，我们此时使用的是db模式，config.txt中的如`store.file.`或`store.redis.`一类的都可以删除，同时需要把`store.db.`对应内容的值填写完整。**
>
> ----------------------------
>
> **另外，一个重点是其中的`service.vgroupMapping.my_test_tx_group=default`，其中的`my_test_tx_group`可以有我们自己随便定义，`default`也可以自己定义，但要与`registry.conf`中指定的`cluster`的值相同 **。
>
> > cluster对应的地址由`service.default.grouplist`，它的值就是对应的地址，默认是`127.0.0.1:8091`，中间的`default`就是cluster的值，如果cluster的值变化了，也需要修改对应的名字。
>
> 这个`service.vgroupMapping`的作用在于，我们可以有几个独立的seata集群在运行，几个都注册在nacos中，客户端可以指定一个`my_test_tx_group`，nacos就会去自己的配置中心查询DataId为`service.vgroupMapping.my_test_tx_group`的值，获取到值为`default`后，通过`service.default.grouplist`的值获取对应的地址，如果是在nacos中注册发现的，也可以获取符合该信息的服务。

由于seata的1.3.0有个小bug，需要在原本的路径下添加一个`logs`的目录，作为日志文件的存放地，[作者好像是忘记打包进去](https://github.com/seata/seata/issues/3534)，导致启动失败，读者添加一下即可，之后到bin目录中启动对应的文件即可。

# 使用

seata的作用是用来尽量保证分布式事务的一致性，这样的话，我们自己的一个小项目就需要在两个地方进行操作。

> 先添加一下需要的依赖，这里默认读者是在之前的项目中建立的module，
>
> ```xml
> <dependency>
>     <groupId>org.springframework.boot</groupId>
>     <artifactId>spring-boot-starter-jdbc</artifactId>
> </dependency>
> <dependency>
>     <groupId>org.springframework.boot</groupId>
>     <artifactId>spring-boot-starter-web</artifactId>
> </dependency>
> <dependency>
>     <groupId>org.mybatis.spring.boot</groupId>
>     <artifactId>mybatis-spring-boot-starter</artifactId>
>     <version>2.1.4</version>
> </dependency>
> <dependency>
>     <groupId>mysql</groupId>
>     <artifactId>mysql-connector-java</artifactId>
>     <version>5.1.47</version> <!--如果读者的mysql是8.0+，自然需要对应的8的版本 -->
> </dependency>
> <dependency>
>     <groupId>com.alibaba</groupId>
>     <artifactId>druid-spring-boot-starter</artifactId>
>     <version>1.2.3</version>
> </dependency>
> <dependency>
>     <groupId>org.projectlombok</groupId>
>     <artifactId>lombok</artifactId>
> </dependency>
> <dependency>
>     <groupId>com.google.code.gson</groupId>
>     <artifactId>gson</artifactId>
> </dependency>
> <dependency>
>     <groupId>org.springframework.cloud</groupId>
>     <artifactId>spring-cloud-starter-openfeign</artifactId>
> </dependency>
> <dependency>
>     <groupId>com.alibaba.cloud</groupId>
>     <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
> </dependency>
> <dependency>
>     <groupId>com.alibaba.cloud</groupId>
>     <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
> </dependency>
> <dependency>
>     <groupId>com.alibaba.cloud</groupId>
>     <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
> </dependency>
> ```

这里，读者可以先实现两个不同数据库的表的操作，如订单服务的订单表加一，而库存表减一。为了业务明显，尽量把这些不同业务的表放在不同的数据库中，

>  **除了我们自己业务的表外，对应的数据库中还需要一个`undo_log`表，负责记录当前的一些操作，用作事务失败进行回滚。**
>
> 该表的构造有官方的sql命令，
>
> ```mysql
> CREATE TABLE `undo_log` (
>   `id` bigint(20) NOT NULL AUTO_INCREMENT,
>   `branch_id` bigint(20) NOT NULL,
>   `xid` varchar(100) NOT NULL,
>   `context` varchar(128) NOT NULL,
>   `rollback_info` longblob NOT NULL,
>   `log_status` int(11) NOT NULL,
>   `log_created` datetime NOT NULL,
>   `log_modified` datetime NOT NULL,
>   `ext` varchar(100) DEFAULT NULL,
>   PRIMARY KEY (`id`),
>   UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
> ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
> ```
>
> 在对应的几个数据库中运行即可。

其次，利用之前的技术，使用mybatis，mvc实现API，一个负责在订单表中增1，一个在库存表中减一。

```
├───controller
├───dao
├───pojo
└───service
    └───impl
```

大致的目录结构如上，pojo负责对象类，dao是基础的映射接口，由resources目录下的xml文件完成完整的实现，service下一样有个服务的接口，其中的impl内完成对应服务的完整的实现，

> 完整的服务实现中，一方面包含订单增加，同时需要调用库存的API完成库存数量的减一。

最后controller就是简单的一个APi实现，负责调用服务实现完整订单的操作。

在项目的启动类上加上注解

```java
@SpringBootApplication
@MapperScan(basePackages = "dao包的路径")
@EnableTransactionManagement//负责事务处理
@EnableFeignClients//启动openfeign
@EnableDiscoveryClient//启动nacos服务发现
```

实现对应的库存服务的一个openfeign接口，并在之前的服务实现中实现库存减1。

> **对应的服务实现方法上加上注解`@GlobalTransactional`，表示用于分布式事务**

我们的项目配置文件，也需要做一些工作，~这里默认使用的是yaml格式~

> ```yaml
> spring:
>   datasource:
>     driver-class-name: com.mysql.jdbc.Driver #这里的数据库版本是5.7，8.0+需要中间加个cj
>     type: com.alibaba.druid.pool.DruidDataSource
>     username: 数据库用户名
>     password: 数据库密码
>     url: jdbc:mysql://数据库地址:3306/orderSeata?characterEncoding=utf-8&useSSL=false
>   application:
>     name: orderSeata-service #随便写个服务名
>   cloud:
>   # 自己作为一个服务注册到nacos中
>     nacos:
>       server-addr: ip:端口
>       discovery:
>         group: 组名
>         namespace: 命名空间的id
>         username: nacos用户名
>         password: 密码
>     alibaba:
>       seata:
>         tx-service-group: 你的可能修改的my_test_tx_group名字
> server:
>   port: 8080 # 自己API对应端口
> 
> # 一些mybaits的设置
> mybatis:
>   type-aliases-package: dao包的路径
>   mapper-locations: classpath:mapper/*.xml 
>   #对应的是resources目录下mapper目录下的xml文件，读者也可以自己随便写个目录 
>   
>   configuration:
>     map-underscore-to-camel-case: true #启用驼峰命名机制，不需要可以不设置
>     
> # 我们该服务作为一个客户端，需要知道运行起来的seata在nacos中的服务位置，以及配置内容的位置 
> #就是把我们自己在file.conf，和registry.conf文件中关于nacos的信息填一遍
> seata:
>   registry:
>     nacos:
>       application: 
>       server-addr: 
>       group: 
>       namespace: 
>       username: 
>       password: 
>     type: nacos
>   config:
>     nacos:
>       group: 
>       server-addr: 
>       namespace: 
>       username: 
>       password: 
>     type: nacos
> #关于openfeign，这里主要设置了一下延迟，默认的太短，导致很容易显示超时而包异常。这里连接和读都设置为5秒
> feign:
>   client:
>     config:
>       default:
>         connectTimeout: 5000
>         readTimeout: 5000
> ```

此时，访问对应的订单API，可以同时处理订单表和库存表，如果中间出现异常，所有操作也会自己回滚。

# 附录

## seata参数配置 1.3.0版本

摘自[Seata 参数配置](http://seata.io/zh-cn/docs/user/configurations.html)

### 变更记录

```
20200716(1.3.0):
1.增加了store.redis相关配置
2.增加了nacos注册中心配置group项,Server和Client端的值需一致
20200421(1.2.0): 
1.增加registry.nacos.application属性，默认seata-server，Server和Client端的值需一致
20200220(1.1.0): 
1.file.conf和registry.conf两个配置文件中的格式统一转换为驼峰格式.
2.统一所有配置文件的默认值(file.conf、registry.conf、seata-spring-boot-starter)
3.优化seata-spring-boot-starter中对于事务分组和TC集群的配置
4.移除client.support.spring.datasource.autoproxy,增加@EnableAutoDataSourceProxy
注解用于开启数据源自动代理,同时可选择代理实现方式(具体请查阅附录5)
20191221: 
1.增加seata.enabled、client.report.success.enable、
transport.enable-client-batch-send-request、client.log.exceptionRate
```

### 关注属性(详细描述见全属性)

| server端                       | client端                               |
| ------------------------------ | -------------------------------------- |
| registry.type                  | registry.type                          |
| config.type                    | config.type                            |
| #store.mode=db需要以下配置     | service.vgroupMapping.my_test_tx_group |
| store.db.driverClassName       | service.default.grouplist              |
| store.db.url                   | service.disableGlobalTransaction       |
| store.db.user                  |                                        |
| store.db.password              |                                        |
| #store.mode=redis 需要以下配置 |                                        |
| store.redis.host               |                                        |
| store.redis.port               |                                        |
| store.redis.database           |                                        |
| store.redis.password           |                                        |

### 全属性

#### 公共部分

| key                     | desc                           | remark                                                       |
| ----------------------- | ------------------------------ | ------------------------------------------------------------ |
| transport.serialization | client和server通信编解码方式   | seata(ByteBuf)、protobuf、kryo、hession、fst，默认seata      |
| transport.compressor    | client和server通信数据压缩方式 | none、gzip，默认none                                         |
| transport.heartbeat     | client和server通信心跳检测开关 | 默认true开启                                                 |
| registry.type           | 注册中心类型                   | 默认file，支持file 、nacos 、eureka、redis、zk、consul、etcd3、sofa、custom |
| config.type             | 配置中心类型                   | 默认file，支持file、nacos 、apollo、zk、consul、etcd3、custom |

#### server端

| key                                       | desc                                             | remark                                                       |
| ----------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------ |
| server.undo.logSaveDays                   | undo保留天数                                     | 默认7天,log_status=1（附录3）和未正常清理的undo              |
| server.undo.logDeletePeriod               | undo清理线程间隔时间                             | 默认86400000，单位毫秒                                       |
| server.maxCommitRetryTimeout              | 二阶段提交重试超时时长                           | 单位ms,s,m,h,d,对应毫秒,秒,分,小时,天,默认毫秒。默认值-1表示无限重试。公式: timeout>=now-globalTransactionBeginTime,true表示超时则不再重试(注: 达到超时时间后将不会做任何重试,有数据不一致风险,除非业务自行可校准数据,否者慎用) |
| server.maxRollbackRetryTimeout            | 二阶段回滚重试超时时长                           | 同commit                                                     |
| server.recovery.committingRetryPeriod     | 二阶段提交未完成状态全局事务重试提交线程间隔时间 | 默认1000，单位毫秒                                           |
| server.recovery.asynCommittingRetryPeriod | 二阶段异步提交状态重试提交线程间隔时间           | 默认1000，单位毫秒                                           |
| server.recovery.rollbackingRetryPeriod    | 二阶段回滚状态重试回滚线程间隔时间               | 默认1000，单位毫秒                                           |
| server.recovery.timeoutRetryPeriod        | 超时状态检测重试线程间隔时间                     | 默认1000，单位毫秒，检测出超时将全局事务置入回滚会话管理器   |
| store.mode                                | 事务会话信息存储方式                             | file本地文件(不支持HA)，db数据库\|redis(支持HA)              |
| store.file.dir                            | file模式文件存储文件夹名                         | 默认sessionStore                                             |
| store.db.datasource                       | db模式数据源类型                                 | dbcp、druid、hikari；无默认值，store.mode=db时必须指定。     |
| store.db.dbType                           | db模式数据库类型                                 | mysql、oracle、db2、sqlserver、sybaee、h2、sqlite、access、postgresql、oceanbase；无默认值，store.mode=db时必须指定。 |
| store.db.driverClassName                  | db模式数据库驱动                                 | store.mode=db时必须指定                                      |
| store.db.url                              | db模式数据库url                                  | store.mode=db时必须指定，在使用mysql作为数据源时，建议在连接参数中加上`rewriteBatchedStatements=true`(详细原因请阅读附录7) |
| store.db.user                             | db模式数据库账户                                 | store.mode=db时必须指定                                      |
| store.db.password                         | db模式数据库账户密码                             | store.mode=db时必须指定                                      |
| store.db.minConn                          | db模式数据库初始连接数                           | 默认1                                                        |
| store.db.maxConn                          | db模式数据库最大连接数                           | 默认20                                                       |
| store.db.maxWait                          | db模式获取连接时最大等待时间                     | 默认5000，单位毫秒                                           |
| store.db.globalTable                      | db模式全局事务表名                               | 默认global_table                                             |
| store.db.branchTable                      | db模式分支事务表名                               | 默认branch_table                                             |
| store.db.lockTable                        | db模式全局锁表名                                 | 默认lock_table                                               |
| store.db.queryLimit                       | db模式查询全局事务一次的最大条数                 | 默认100                                                      |
| store.redis.host                          | redis模式ip                                      | 默认127.0.0.1                                                |
| store.redis.port                          | redis模式端口                                    | 默认6379                                                     |
| store.redis.maxConn                       | redis模式最大连接数                              | 默认10                                                       |
| store.redis.minConn                       | redis模式最小连接数                              | 默认1                                                        |
| store.redis.database                      | redis模式默认库                                  | 默认0                                                        |
| store.redis.password                      | redis模式密码(无可不填)                          | 默认null                                                     |
| store.redis.queryLimit                    | redis模式一次查询最大条数                        | 默认100                                                      |
| metrics.enabled                           | 是否启用Metrics                                  | 默认false关闭，在False状态下，所有与Metrics相关的组件将不会被初始化，使得性能损耗最低 |
| metrics.registryType                      | 指标注册器类型                                   | Metrics使用的指标注册器类型，默认为内置的compact（简易）实现，这个实现中的Meter仅使用有限内存计数，性能高足够满足大多数场景；目前只能设置一个指标注册器实现 |
| metrics.exporterList                      | 指标结果Measurement数据输出器列表                | 默认prometheus，多个输出器使用英文逗号分割，例如"prometheus,jmx"，目前仅实现了对接prometheus的输出器 |
| metrics.exporterPrometheusPort            | prometheus输出器Client端口号                     | 默认9898                                                     |

#### client端

| key                                                | desc                                        | remark                                                       |
| -------------------------------------------------- | ------------------------------------------- | ------------------------------------------------------------ |
| seata.enabled                                      | 是否开启spring-boot自动装配                 | true、false,(SSBS)专有配置，默认true（附录4）                |
| seata.enableAutoDataSourceProxy=true               | 是否开启数据源自动代理                      | true、false,seata-spring-boot-starter(SSBS)专有配置,SSBS默认会开启数据源自动代理,可通过该配置项关闭. |
| seata.useJdkProxy=false                            | 是否使用JDK代理作为数据源自动代理的实现方式 | true、false,(SSBS)专有配置,默认false,采用CGLIB作为数据源自动代理的实现方式 |
| transport.enableClientBatchSendRequest             | 客户端事务消息请求是否批量合并发送          | 默认true，false单条发送                                      |
| client.log.exceptionRate                           | 日志异常输出概率                            | 默认100，目前用于undo回滚失败时异常堆栈输出，百分之一的概率输出，回滚失败基本是脏数据，无需输出堆栈占用硬盘空间 |
| service.vgroupMapping.my_test_tx_group             | 事务群组（附录1）                           | my_test_tx_group为分组，配置项值为TC集群名                   |
| service.default.grouplist                          | TC服务列表（附录2）                         | 仅注册中心为file时使用                                       |
| service.disableGlobalTransaction                   | 全局事务开关                                | 默认false。false为开启，true为关闭                           |
| client.tm.degradeCheck                             | 降级开关                                    | 默认false。业务侧根据连续错误数自动降级不走seata事务(详细介绍请阅读附录6) |
| client.tm.degradeCheckAllowTimes                   | 升降级达标阈值                              | 默认10                                                       |
| client.tm.degradeCheckPeriod                       | 服务自检周期                                | 默认2000,单位ms.每2秒进行一次服务自检,来决定                 |
| client.rm.reportSuccessEnable                      | 是否上报一阶段成功                          | true、false，从1.1.0版本开始,默认false.true用于保持分支事务生命周期记录完整，false可提高不少性能 |
| client.rm.asynCommitBufferLimit                    | 异步提交缓存队列长度                        | 默认10000。 二阶段提交成功，RM异步清理undo队列               |
| client.rm.lock.retryInterval                       | 校验或占用全局锁重试间隔                    | 默认10，单位毫秒                                             |
| client.rm.lock.retryTimes                          | 校验或占用全局锁重试次数                    | 默认30                                                       |
| client.rm.lock.retryPolicyBranchRollbackOnConflict | 分支事务与其它全局回滚事务冲突时锁策略      | 默认true，优先释放本地锁让回滚成功                           |
| client.rm.reportRetryCount                         | 一阶段结果上报TC重试次数                    | 默认5次                                                      |
| client.rm.tableMetaCheckEnable                     | 自动刷新缓存中的表结构                      | 默认false                                                    |
| client.tm.commitRetryCount                         | 一阶段全局提交结果上报TC重试次数            | 默认1次，建议大于1                                           |
| client.tm.rollbackRetryCount                       | 一阶段全局回滚结果上报TC重试次数            | 默认1次，建议大于1                                           |
| client.undo.dataValidation                         | 二阶段回滚镜像校验                          | 默认true开启，false关闭                                      |
| client.undo.logSerialization                       | undo序列化方式                              | 默认jackson                                                  |
| client.undo.logTable                               | 自定义undo表名                              | 默认undo_log                                                 |



## 协议/模式



强一致性的处理分布式事务的组件，内部包含大量的锁，主要被蚂蚁金服使用，因为锁太多影响性能，但安全。





两阶段提交协议，进化版三阶段协议。阻塞参与者的资源。

> 一阶段：预处理，参与者进行日志填写，undo,redo,并返回ask
>
> 二阶段:提交，如果一个参与者失败，就无法保证成功。



AT模式，

> 一阶段：拦截业务sql，解析语义，提取操纵的记录，将其保存为before image，再执行sql语句。
>
> 对应的结果保存为after image,生成行锁。此时已经完成提交。
>
> > 行锁，避免脏读
>
> 二阶段：如果没问题，就只需要删除快照和行锁即可。或者利用before image 进行回滚。

TCC模式，



可靠消息一致性，

使用MQ，RocketMQ与TCC模式类似，需要自己定义try,confirm,cancel，并且有一个回查机制，默认15次，否则删除对应的消息，最后投递消息，commit,不成功会重试



- 角色

  TC:事务协调者

  TM:事务管理器

  RM:资源管理器

  

