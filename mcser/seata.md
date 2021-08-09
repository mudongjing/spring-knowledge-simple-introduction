# 安装

首先，读者应该已经下载了对应的压缩文件，解压即可。

我们需要稍微做一下配置，对`conf`目录下的几个文件的内容做修改

> - file.conf
>
>   内部的主要内容是存储模式和该存储模式的设置。默认是`file`，是在本机内存中存储数据。另外还有`db`和`redis`模式。如果是单机使用，自然使用file即可，当然使用数据库也挺好，如果是集群模式，则最好使用`db`模式，即使用数据库，`redis`也可以。更深入的，我们也可能需要进一步的将数据库或redis进行集群化。
>
>   简单示例
>
>   ```
>   store {
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
>   















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

  