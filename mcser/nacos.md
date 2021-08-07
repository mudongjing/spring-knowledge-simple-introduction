# Nacos安装

作为一个微服务的组件，具有集群模式是最基本的，只不过我们自己使用时可以用单机模式。

解压下载的文件，我们可以在`/conf/application.properties`的文件中，修改端口号，或添加数据库的地址。

在`/bin`目录中，可以运行`startup`文件，比如运行为单机模式

```bash
#在对应的bin目录下
sh startup.sh -m standalone
```

默认是集群模式，直接运行即可，

在这个启动文件中，我们可以修改JVM设置

```shell
#========================================================================================
# JVM Configuration
#========================================================================================
if [[ "${MODE}" == "standalone" ]]; then
    JAVA_OPT="${JAVA_OPT} -Xms512m -Xmx512m -Xmn256m"
    JAVA_OPT="${JAVA_OPT} -Dnacos.standalone=true"
else
    if [[ "${EMBEDDED_STORAGE}" == "embedded" ]]; then
        JAVA_OPT="${JAVA_OPT} -DembeddedStorage=true"
    fi
    JAVA_OPT="${JAVA_OPT} -server -Xms2g -Xmx2g -Xmn1g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=320m"
    JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${BASE_DIR}/logs/java_heapdump.hprof"
    JAVA_OPT="${JAVA_OPT} -XX:-UseLargePages"
fi
```

为对应的JVM设置合适的内存空间大小。

-----------------

- 数据库

  如果打算支持数据库，读者以防万一先把nacos的源码下载到本地，其中sql文件`distribution/conf/nacos-mysql.sql`是官方的一个数据库初始化示例，可以在自己的mysql中运行该文件。

  然后在`/conf/application.properties`文件中，添加上对应的信息

  ```properties
  ### Connect URL of DB:
  #示例代码
  db.url.0=jdbc:mysql://127.0.0.1:3306/nacos?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=UTC
  db.user.0=nacos
  db.password.0=nacos
  ```

- 集群模式

  如果要建立一个集群，官方认为至少有3个节点才行。

  将集群中的所有节点`ip:端口`信息放在`application.properties`文件的尾部

  如官方示例

  ```properties
  # ip:port
  200.8.9.16:8848
  200.8.9.17:8848
  200.8.9.18:8848
  ```

  > 进一步的，我们最好将数据库也设置为集群模式

  现在，我们只需要在对应的机器上把所有的设置配置好即可，启动后，例如使用`http://200.8.9.18:8848/nacos/`，即可登录到nacos的页面，输入用户名nacos和密码nacos即可进入。

  ---------------

  - 代理

    现在的一个问题在于，如果只是这样，那么访问nacos具体使用哪个机器上的，又如何保证各机器的负载较为平均。此时就需要nginx负责。

    可以再添加一台机器，安装nginx，在对应的`conf.d`文件中，写入以下的内容

    ```properties
    upstream nacoscluster{
    	server ip:端口 
    	# 这是nacos对应的地址
    	server ip:端口
    }
    server{
    	listen  端口; 
        # 我们设置nginx在该机器上的监听端口
    	server_name localhost;
    	# 我们不需要再关心多个nacos的地址，而是使用nginx的地址 http://localhost:端口/nacos/
    	location /nacos/{
    		proxy_pass http://nacoscluster/nacos/;
    		# 我们访问了nginx的地址，nginx就会去访问前面指定的某个nacos的机器，默认就是轮询机制
    	}
    }
    ```

    > 读者如果希望在自己的机器上，尝试建立一个集群，自然可以使用多个不同的端口。
    >
    > > 但根据我的使用，nacos自定义的端口最好使用不小于8848的数字。否则可能nacos启动失败。

# nacos 使用

> 可参考[nacos官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)的介绍。

由于nacos有可视化界面，许多操作都可以在对应的界面中设置和修改。

- ==登录==

  首先，进入页面后，读者可以修改用户列表，编辑登录密码，也可增加新用户。

  ![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/ui/user.png)

  我们可以定义角色，指明该角色具有的权限，

  ![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/ui/permit.png)

  在为不同的用户绑定为符合其权限控制的角色，

  ![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/ui/role.png)

- ==分组==

  我们的服务或其它的一些配置项，随着业务的膨胀也会日益复杂，则需要为这些内容进行分类和划分。

  比如在大的方面，分为测试环境，生产环境，内部又分为针对各组件的分组。

  这种分类就对应着命名空间和组

  ```mermaid
  graph TD
  subgraph nacos
  	subgraph namespace1
  		subgraph group1
  		end
  		subgraph group2
  		end
  	end
  	subgraph namespace2
  		subgraph group3
  		end
  		subgraph group4
  		end
  	end
  end
  ```

  我们可以定义几个需要的命名空间，

  ![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/ui/namespace.png)

  然后就是可以在创建服务或配置内容时可以自己随便填写一个`Group`的名字。

- ==配置管理==

  从图中就可以看出，这些配置实际已经从属于不同的命名空间，而又具有自己的Group名

  ![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/ui/config.png)

  创建一个配置的时候，如下图，可以设置基本的信息，而具体的配置内容可以填写在`配置内容`中，而配置内容的格式也提供了多种。

  > 这里的配置内容就相当于对应组件的配置文件

  ![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/ui/edit-config.png)

- ==服务==

  我们可以查看存在的服务，这里我们是创建的一个空服务。

  如果是运行中的服务，我们可以进入它的详情，看到该服务对应的几个不同的机器，并对某个机器进行流量控制，其中的元数据可以让我们按照K-V格式存储我们希望保存的信息。

  ![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/ui/service.png)

- ==集群==

  可以查看nacos集群中的节点信息，能够显示对应节点的健康状况

  ![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/ui/cluster_node.png)





# Nacos 项目

接下来，我们需要了解一下，如何利用代码在nacos中注册服务或管理配置内容。

读者也可能发现，前面的图片中，有不少包含·示例代码·的字样，这些示例代码就是提供了一个对应的简单的客户端代码。

![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/ui/sample.png)

## nacos discovery





## nacos config





- nacos

雪崩保护 对应保护阈值.





nacos的配置文件中，需要nacos.core.auth.enabled=true，一允许实现权限控制。

nacos-config 使用md5去感知对应服务的配置是否变化，每个10ms,改变则拉取一次。【bug,对于public类型的，产生的md5不同】

