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

读者应该也已经下载了seata项目的源码，我们需要做的就是把源码中的`script/config-center/config.txt`文件的内容提交给nacos，`script/config-center/nacos`目录下有`nacos-config.py`和 `nacos-config.sh`两个文件，均可用于自动完成内容配置的任务，

> linux上或macos上一般都是可以直接运行sh脚本文件，前提是你的nacos的ip和端口都是默认的，否则需要额外做修改工作，如果windows，可以安装git客户端，在其中运行sh脚本，或是安装python，执行py脚本。

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
>   > 读者最好为seata单独创建一个命名空间，seata的配置内容还是有点恶心的，把config.txt中的内容每行都建立一个。
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
>   for line in open('../config.txt'):# 同样读取上层目录的文件
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
>   但似乎没有指定用户名和密码的指令，读者可以参考shell脚本中的API格式在原有的其中的地址上添加上类似`&username=$username&password=$password`的附加参数，\$username或\$​password需要换成自己设定的变量或直接写上自己的值。

由于seata的1.3.0有个小bug，需要在原本的路径下添加一个`logs`的目录，作为日志文件的存放地，[作者好像是忘记打包进去](https://github.com/seata/seata/issues/3534)，导致启动失败，读者添加一下即可，之后到bin目录中启动对应的文件即可，类似于

```bash
sh seata-server.sh -p 端口 -h 地址
```

# 使用







# 附录



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

  