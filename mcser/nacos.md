

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

> 更多配置文件的信息可以看一下[Nacos系统参数介绍](#Nacos系统参数介绍)

# nacos 使用

> 可参考[nacos官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)的介绍。

由于nacos有可视化界面，许多操作都可以在对应的界面中设置和修改。

- ==登录==

  首先，进入页面后，读者可以修改用户列表，编辑登录密码，也可增加新用户。

  我们希望的是能够使用自定义的用户名和密码进行登录，需要在配置文件中修改

  ```properties
  nacos.core.auth.enabled=true # 默认是false
  ```

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

基于README中创建的项目，新键一个module，使用maven不需要依赖任何架构。

对应的pom中，添加这几个依赖

```xml
<!-- 因为父项目已经指定了alibaba,cloud等父依赖，其子项目不需要再特意指定版本-->

<!-- 我们使用API的基础-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- 这里对应的应该是2.2.6.RELEASE版本,内部已包含了ribbon，可提供负载均衡的功能-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>

<!-- 不是一个特别必要的依赖，主要是简化我们使用API发送命令的操作-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<!-- 由于API主要是指定一个http地址，我们需要把一些指定的参数与对应的url地址拼接，再完成发送-->
<!-- 导致我们需要额外RestTemplate进行这种url的繁琐操作-->
<!-- 而openfeign则相当于Mybaits，API本身就是对应着那个服务的controller，只要指明是哪个服务的哪个controller即可-->
<!--openfeign就是基于nacos内部注册了所有的服务，可以通过一个接口的声明，指定使用的具体方法-->
```

假设我们要实现一个订单处理的服务，那么定义这个module的启动类为orderApplication

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class OrderApplication {
    public static void main(String[] args){
        SpringApplication.run(OrderApplication.class,args);
    }
}
```

> 如果我们试图使用·RestTemplate·来做API的调用，则需要在RestTemplate实例创建的类上加上注解@LoadBalanced，以识别服务名并进行转发。

我们这里使用的是openfeign的方法。

> 假设，现在有一个类似结构的module，负责的是库存数量，对应的服务名是·stock-service·，对应的controller是

> ```java
> @RestController
> @RequestMapping("/stock")
> public class StockController {
>     @RequestMapping("/reduce")
>     public String reduce(){
>         System.out.println("库存减一");
>         return "仓库拣货";
>     }
> }
> //如果是RestTemplate方法，我们需要拼接处字符串 "http://stock-service/stock/reduct"
> ```

那么openfeign，则需要建立一个接口，假设名字是·StockFeignService·，

```java
@FeignClient(name="stock-service",path="/stock")
public interface StockFeignService {
    @RequestMapping("/reduce")
    String reduce();
}
//就基本是把对方的controller的类去掉方法实现
```

随便写一个负责订单的controller，

```java
@RestController
@RequestMapping("/order")
public class OrderController {
    private final StockFeignService stockFeignService;
    @Autowired
    public OrderController(StockFeignService stockFeignService){
        this.stockFeignService=stockFeignService;
    }
    
    @RequestMapping("/add")
    public String add (){
        System.out.println("下单");
        String msg=stockFeignService.reduce();//直接使用接口中的方法即可
        return "用户下单"+msg;
    }
}
```

> 上面说明了基本的代码编写

有了上面构建的API，我们需要把这些注册到nacos中，此时，在`resources`目录下创建`application.yml` 文件,填写对应的信息，

```yaml
server:
  port: 端口 # 这是该服务自己的地址端口
spring:
  application:
    name: order-service #服务的名称，将被nacos用于注册，随便写一个
  cloud:
    nacos:
      server-addr: ip:端口 # nacos的地址，如果是使用了nginx代理，则填写对应的nginx的监听地址
      discovery:
        username: nacos #我们可以填写指定的用户和密码
        password: nacos
        namespace: 837d0dbd-f8ca-4289-990b-4f097ea6f4ea #默认是public，其它的需要写对应的id
        group: orderGroup #可以随便写一个
```

下面就是我们自己定义了一个`dev`的命名空间，

![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/discovery/namespace.png)

启动后，可以在dev命名空间下发现我们的服务，

![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/discovery/orderservice-dev.png)

进入详情中，可以看到一些保护阈值之类的数据，如果我们有多个该服务的实例，下面的集群中自然会有多个节点。

我们也可以针对各个节点修改对应的权值之类的数据。

![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/discovery/order-details.png)

1. 服务实例的权重：用于指定该实例可获得的流量大小。权重越大，则可获得更多的流量。

2. 保护阈值：该值是一个[0,1]区间的浮点数。用于处理健康实例占总实例数的比例，

   > 首先是健康实例，自然就是那些能够正常负责用户请求的实例，也自然存在一些可能因为各种原因反正就是无法正常工作的实例。
   >
   > 这里存在一个雪崩效应的概念，即对于服务的需求较大，而不幸的是已经有不少实例无法正常工作，正常而言，必然需要把这些需求交由健康实例负责，这样又把健康实例的负载撑坏，导致整个服务崩溃。
   >
   > 为了避免雪崩效应，就需要避免不利环境下的进一步自我摧残。
   >
   > 当发现健康实例的占比小于保护阈值后，将无视实例的健康与否，都可以用来负责用户的请求。
   >
   > 此时，必然存在一些请求无法处理，但保证了整个服务的可运行。

------------------------------

> 需要注意的是，命名空间和分组会隔绝服务，如果我们希望能够调用库存服务，那么库存服务的命名空间和分组需要与订单服务相同，否则无法获取对方的存在。



## nacos config

> 现在我们试图向nacos中发布自己的一个配置内容，这一操作之所以重要，因为以后我们的一个项目可能需要各种的组件共同完成一个任务，而有些组件的配置内容是会根据场景而变化，于是，我们需要一个可以存放各个组件动态变化的配置内容的地方，而这个地方有最好方便组件启动时进行配置内容的读取。

首先我们需要在页面中配置一个可以使用的配置内容

> 读者的配置内容最好也放在自己定义的命名空间中，因为默认的public有些许的小bug。

下面是我们建立的一个配置

![](https://mudongjing.github.io/gallery/cloud/alibaba/nacos/config/orderconfig.png)

```properties
Data ID= orderConfig-dev.yaml
# data id 有它的一个命名规则
# ${prefix}-${spring.profiles.active}.${file-extension}
# ${prefix}可能有很多，用于各种环境，如生产环境或测试环境
# ${spring.profiles.active}就是用来指定不同的环境，
Group= orderGroup
# MD5 是系统自己生成的
配置格式=YAML
配置内容={
	user:
   		name: nacos_order
    	password: nacos_mima 
}
# 并且放在之前已经定义好的·dev·命名空间中
```

首先正常发创建一个module，配置文件此时比较特殊，有两个

bootstrap.yml

```yaml
spring:
  application:
    name: orderConfig #与对应的配置内容的 data id保持一致，这里·-dev.yaml·属于额外的内容
    #使用${prefix}即可
    
  cloud:
    nacos:
      server-addr: nacos的ip:端口
      # 此时开启权限控制，必须写上用户名和密码
      username: nacos #也可以随便写自己定义的用户名和密码
      password: nacos
      config:
        namespace: 如果命名空间是public，可以不写，否则写上 对应命名空间的 id值
        file-extension: yaml #指明 使用的配置格式，默认是properties
        group: orderGroup # 读取配置所对应的组
        refresh-enabled: true #默认就是true，可动态读取对应的配置内容，一般不用写，这里只是介绍一下
        
#        shared-configs: # 读取共享的配置文件，是一个数组类型
#          - data-id: # 对应配置的文件名
#            group: # 对应的组名
#            refresh: true # 动态感知

		# 与shared-configs 类似的，里面的内容也是一致的，extension配置文件的优先级大于shared的文件
        # extension-configs:
```

application.yml

```yaml
server:
  port: 8090 #自己随便写一个
spring:
  profiles:
    active: dev #用于指明${spring.profiles.active}
```

我们再写一个读取配置内容的controller类

```java
@RestController
@RequestMapping("/config")
@RefreshScope //能够让从对应配置文件中利用@value获取的内容动态刷新
public class NacosConfigController {
    @Value("${user.name}")//把nacos中的配置内容看作本地的配置文件即可
    private String userName;
    @Value("${user.password}")
    private String userPassword;

    @RequestMapping("/get")
    public String config(){
        return "用户名"+userName+"，密码:  "+userPassword;
    }
}
```

启动项目后，就可以在对应的地址上获取到对应的内容。修改配置内容在访问，又可以得到新的结果。

> nacos-config 使用md5去感知对应服务的配置是否变化，每隔10ms检查一次，改变则将内容拉取一次。【bug,对于public类型的，不改变，产生的md5也不同】



# 附录

## Nacos系统参数介绍

### server



#### naming



#### config



#### CMDB



### client

#### 通用



#### naming



#### config







## openAPI

nacos提供的openAPI是一些相对于直接注册服务较为内部的功能，简单而言，是一些可以不依赖界面就对nacos进行操作的手段。

### 配置

- 获取配置



- 监听配置



- 发布配置





- 删除配置





- 历史版本

  

  - 版本详情







### 发现

- 注册实例





- 注销实例





- 修改实例







- 实例列表





- 实例详情





- 心跳



- 创建服务





- 删除服务





- 修改服务





- 查询服务





- 服务列表





- 系统开关





- 修改开关





- 数据指标





- 集群server列表



- 集群leader



- 健康状态



- 更新元数据



- 删除元数据

### 命名空间

- 命名空间列表



- 创建命名空间



- 修改命名空间





- 删除命名空间



