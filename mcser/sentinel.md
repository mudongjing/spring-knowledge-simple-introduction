# 安装

读者应该在阅读README使下载了对应的jar包，简单而言

```bash
java -jar sentinel-dashboard-1.8.1.jar 
```

此时，默认的是`8080`端口，如果没有冲突，读者此时可以登录进去看一下，默认用户名和密码是`sentinel`。

此时，页面中空空如也，因为还没有任何流量需要它拦截，它也懒得摆出相关的设置。

如果需要自己指定对应的端口号，自然可以使用对应的参数修改，但我们可以在一个配置文件中明确各项参数，只需要启动时指定该文件即可。

> 为方便，我们就为sentinel建立一个目录，将jar包和配置文件放入其中。

==如果想临时修改某个值，就还是使用命令行参数指定即可，会覆盖文件中的值。==

我们就让配置文件名为`sentinel.properties`，启动命令就变成，

```bash
java -Dcsp.sentinel.config.file=sentinel.properties -jar sentinel-dashboard-1.8.1.jar 
```

> ~需要注意的是，配置文件的编码需要是UTF-8~
>
> 说是这么说，但我在使用时，使用配置文件不能修改服务端口，与其使用配置文件，不如直接写一个脚本运行。

配置选项可以查看附录的[配置项](#配置项)。这些配置项，前面拼上-D就是命令行的参数。

# 使用

> 其实，sentinel可以不去学习的那么深入，相比于以往的很多组件，主要需要使用代码进行设置，sentinel很多功能都可以在其控制台的页面中进行操作。

## 仅使用对应的依赖

sentinel作为一个组件，与alibaba cloud 仅为合作关系，我们可以单纯地使用sentinel做一个降级处理。

读者可以新建一个项目，或再创建一个module，总之此时可以不依赖cloud的关系。

下面简单给一个基本的项目内容

pom文件中引入sentinel的几个依赖，以及springBoot的web依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.12</version>
</dependency>

<!-- sentinel核心库-->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-core</artifactId>
    <version>1.8.1</version>
</dependency>

<!--可使用@SentinelResource-->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-annotation-aspectj</artifactId>
    <version>1.8.1</version>
</dependency>
```

读者自己随便写个启动类。

appication.yml，写入`server.port`的端口号即可。

写一个controller的类

```java
@RestController
@Slf4j
public class SentinelController {
    private static final String RESOURCE_NAEM="hello";
    @RequestMapping("/hello")
    public String hello(){
        Entry entry=null;
        try{//正常，不被流控时，将执行下述命令
            //定义资源名称
            entry = SphU.entry(RESOURCE_NAEM);
            String str="hello world";
            log.info("==="+str+"===");
            return str;
        }catch(Exception e){//当触发流控降级，执行下述命令
            log.info("block!");
            Tracer.traceEntry(e,entry);
            return "被流控";
        }finally {
            if(entry!=null){
                entry.exit();
            }
        }
    }

    @PostConstruct //在创建该类时，会自动运行该方法，进行初始化
    private static void initRules(){
        //流控规则列表
        List<FlowRule> rules=new ArrayList<>();
        //流控
        FlowRule rule=new FlowRule();
        //设置受保护的资源，这里指明的就是针对上面的那个资源
        rule.setResource(RESOURCE_NAEM);
        //设置规则
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);//基于流量降级
        rule.setCount(1);//当1秒中多余1各请求，就触发降级
        rules.add(rule);
        //加载配置的规则
        FlowRuleManager.loadRules(rules);
    }
}
```

此时，运行启动类，打开`http://localhost:端口/hello`，就可以看到返回"hello world"字样，当我们请求的快一点，就会触发降级，并返回"被流控"的字样。

> 除此之外，使用该代码手段可以完成界面所有可能规则的操作，甚至是界面做不到的事情。

-----------------------

现在我们基本可以知道如何使用java代码简单完成以下规则的设置，上面是一种使用方式，只不过try，catch之类的语句让代码没那么清晰，我们下面再用注解的方式，@SentinelResource实现一个规则控制，

> 为了让结构明朗，我们需要定义
>
> - 一个pojo类，用于作为消息的载体，
> - 一个类似于上述的controller类，
> - 还有一个触发规则后执行操作的blockHandler方法。
> - 一个发生异常进行操作的fallback方法
>
> 乍一看，复杂了很多，但实际各司其职，更易于理解。
>
> 首先我们随便定义一个User类，之后的方法均会用这个对象返回消息，
>
> ```java
> @NoArgsConstructor
> @Getter
> @Setter
> public class User {
>     private String id;
>     public User(String id){
>         this.id=id;
>     }
> }
> ```
>
> controller类
>
> ```java
> @RestController
> public class SentinelController {
>     private static final String USER_RESOURCE_NAME="user";
> 
>     @RequestMapping("/user")
>     //value设置资源
>     //blockHandler 设置流控降级后的处理方法,【默认】该方法必须一起声明在该类中
>         //如果该方法是放在其它类中，则需要用blockHandlerClass =指明那个类，
>         //需要注意的是，对应的方法需要是static，不然反射是无法获取到
>     //fallback 同样指定一个方法，可用于当接口异常时调用
>         // exceptionsToIgnore 用于排除一些异常处理
>     @SentinelResource(
>             value=USER_RESOURCE_NAME,
>             blockHandler = "blockHandlerForGetUser",
>             blockHandlerClass = BlockHandler.class,
>             fallback = "fallbackHandlerForGetUser",
>             fallbackClass = FallbackHandler.class,
>             exceptionsToIgnore = {ArithmeticException.class})
>     // 加入内部出现某种操作异常，就会调用fallback
>     //流控的优先级高于异常处理
>     public User getUser(String id){
>         return new User("我的名字");
>     }
> 
>    @PostConstruct //在创建该类时，会自动运行该方法，进行初始化
>     private static void initRules(){
>         List<FlowRule> rules=new ArrayList<>();
>         FlowRule rule=new FlowRule();
>         rule.setResource(USER_RESOURCE_NAME);
>         rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
>         rule.setCount(1);
>         rules.add(rule);
>         FlowRuleManager.loadRules(rules);
>     }
> }
> ```
>
> 对应的`BlockHandler`类
>
> ```java
> public class BlockHandler {
>     public static User blockHandlerForGetUser(String id, BlockException e){
>         e.printStackTrace();
>         return new User("流控！！");
>     }
> }
> ```
>
> 对应的`FallbackHandler`类
>
> ```java
> public class FallbackHandler {
>     public static User fallbackHandlerForGetUser(String id,Throwable e){
>         e.printStackTrace();
>         return new User("异常！！");
>     }
> }
> ```
>
> 我们直接在dos窗口用`curl`命令方法对应的地址，
>
> ![](https://mudongjing.github.io/gallery/cloud/alibaba/sentinel/SentinelResource.png)
>
> 同样的，当我们命令快一点，也会触发规则。不同的是，返回的信息是json格式。

## 可用规则

下面部分摘自官方文档。主要介绍可以配置的选项，以及简单的规则设置代码。

- 流量控制

  > 名词，`QPS`，即Queries per second，每秒的访问量。

  |      Field      | 说明                                                         | 默认值                        |
  | :-------------: | :----------------------------------------------------------- | :---------------------------- |
  |    resource     | 资源名，资源名是限流规则的作用对象                           |                               |
  |      count      | 限流阈值                                                     |                               |
  |      grade      | 限流阈值类型，QPS 模式（1）或并发线程数模式（0）             | QPS 模式                      |
  |    limitApp     | 流控针对的调用来源                                           | `default`，代表不区分调用来源 |
  |    strategy     | 调用关系限流策略：直接、链路、关联                           | 根据资源本身（直接）          |
  | controlBehavior | 流控效果（直接拒绝/WarmUp/匀速+排队等待），不支持按调用关系限流 | 直接拒绝                      |
  |   clusterMode   | 是否集群限流                                                 | 否                            |

  ```java
  private void initFlowQpsRule() {
      List<FlowRule> rules = new ArrayList<>();
      FlowRule rule = new FlowRule(resourceName);
      // set limit qps to 20
      rule.setCount(20);
      rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
      rule.setLimitApp("default");
      rules.add(rule);
      FlowRuleManager.loadRules(rules);
  }
  ```

  > ---------------------
  >
  > - **直接拒绝**（`RuleConstant.CONTROL_BEHAVIOR_DEFAULT`）方式是默认的流量控制方式，
  >
  >   - 当QPS超过任意规则的阈值后，新的请求就会被立即拒绝，拒绝方式为抛出`FlowException`。这种方式适用于对系统处理能力确切已知的情况下，比如通过压测确定了系统的准确水位时。
  >
  > - **Warm Up**（`RuleConstant.CONTROL_BEHAVIOR_WARM_UP`）方式，即预热/冷启动方式。
  >
  >   - 当系统长期处于低水位的情况下，当流量突然增加时，直接把系统拉升到高水位可能瞬间把系统压垮。通过"冷启动"，让通过的流量缓慢增加，在一定时间内逐渐增加到阈值上限，给冷系统一个预热的时间，避免冷系统被压垮。
  >
  > - **匀速排队**（`RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER`）方式会严格控制请求通过的间隔时间，也即是让请求以均匀的速度通过，对应的是漏桶算法。
  >
  >   ![image](https://user-images.githubusercontent.com/9434884/68292442-d4af3c00-00c6-11ea-8251-d0977366d9b4.png)
  >
  >   ​	*这种方式主要用于处理间隔性突发的流量，例如消息队列。想象一下这样的场景，在某一秒有大量的请求到来，而接下来的几秒则处于空闲状态，我们希望系统能够在接下来的空闲期间逐渐处理这些请求，而不是在第一秒直接拒绝多余的请求。*
  >
  >   > 注意：匀速排队模式暂时不支持 QPS > 1000 的场景。

  -----------------

> *调用关系包括调用方、被调用方；一个方法又可能会调用其它方法，形成一个调用链路的层次关系。Sentinel 通过 `NodeSelectorSlot` 建立不同资源间的调用的关系，并且通过 `ClusterBuilderSlot` 记录每个资源的实时统计信息。*
>
> - 直接：直接限制对该资源的任何访问
>
> > 可通过以下命令来展示不同的调用方对同一个资源的调用数据：
> >
> > ```shell
> > curl http://localhost:8719/origin?id=nodeA
> > ```
> >
> > 调用数据示例：
> >
> > ```
> > id: nodeA
> > idx origin  threadNum passedQps blockedQps totalQps aRt   1m-passed 1m-blocked 1m-total 
> > 1   caller1 0         0         0          0        0     0         0          0
> > 2   caller2 0         0         0          0        0     0         0          0
> > ```
> >
> > 上面这个命令展示了资源名为 `nodeA` 的资源被两个不同的调用方调用的统计。
> >
> > 流控规则中的 `limitApp` 字段用于根据调用来源进行流量控制。该字段的值有以下三种选项，分别对应不同的场景：
> >
> > - `default`：表示不区分调用者，来自任何调用者的请求都将进行限流统计。如果这个资源名的调用总和超过了这条规则定义的阈值，则触发限流。
> > - `{some_origin_name}`：表示针对特定的调用者，只有来自这个调用者的请求才会进行流量控制。例如 `NodeA` 配置了一条针对调用者`caller1`的规则，那么当且仅当来自 `caller1` 对 `NodeA` 的请求才会触发流量控制。
> > - `other`：表示针对除 `{some_origin_name}` 以外的其余调用方的流量进行流量控制。例如，资源`NodeA`配置了一条针对调用者 `caller1` 的限流规则，同时又配置了一条调用者为 `other` 的规则，那么任意来自非 `caller1` 对 `NodeA` 的调用，都不能超过 `other` 这条规则定义的阈值。
> >
> > 同一个资源名可以配置多条规则，规则的生效顺序为：**{some_origin_name} > other > default**
>
> - 链路：可以指定调用方，仅会对这个调用方施加规则，而无视其它调用方。
>
>   > 配置选项中，web-context--unify:false 默认链路收敛，我们需要设置为true,
>
> - 关联：我们可以当前资源设置所谓的关联资源，当这个关联资源触发规则，受限制的反而是我们当前的资源。

- 熔断降级

  |       Field        | 说明                                                         | 默认值     |
  | :----------------: | :----------------------------------------------------------- | :--------- |
  |      resource      | 资源名，即规则的作用对象                                     |            |
  |       grade        | 熔断策略，支持慢调用比例/异常比例/异常数策略                 | 慢调用比例 |
  |       count        | 慢调用比例模式下为慢调用临界 RT（超出该值计为慢调用）；异常比例/异常数模式下为对应的阈值 |            |
  |     timeWindow     | 熔断时长，单位为 s                                           |            |
  |  minRequestAmount  | 熔断触发的最小请求数，请求数小于该值时即使异常比率超出阈值也不会熔断（1.7.0 引入） | 5          |
  |   statIntervalMs   | 统计时长（单位为 ms），如 60*1000 代表分钟级（1.8.0 引入）   | 1000 ms    |
  | slowRatioThreshold | 慢调用比例阈值，仅慢调用比例模式有效（1.8.0 引入）           |            |

  ```java
  private void initDegradeRule() {
      List<DegradeRule> rules = new ArrayList<>();
      DegradeRule rule = new DegradeRule();
      rule.setResource(KEY);
      // set threshold RT, 10 ms
      rule.setCount(10);
      rule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
      rule.setTimeWindow(10);
      rules.add(rule);
      DegradeRuleManager.loadRules(rules);
  }
  ```

- 系统保护

  |       Field       | 说明                                   | 默认值      |
  | :---------------: | :------------------------------------- | :---------- |
  | highestSystemLoad | `load1` 触发值，用于触发自适应控制阶段 | -1 (不生效) |
  |       avgRt       | 所有入口流量的平均响应时间             | -1 (不生效) |
  |     maxThread     | 入口流量的最大并发数                   | -1 (不生效) |
  |        qps        | 所有入口资源的 QPS                     | -1 (不生效) |
  |  highestCpuUsage  | 当前系统的 CPU 使用率（0.0-1.0）       | -1 (不生效) |

  ```java
  private void initSystemRule() {
      List<SystemRule> rules = new ArrayList<>();
      SystemRule rule = new SystemRule();
      rule.setHighestSystemLoad(10);
      rules.add(rule);
      SystemRuleManager.loadRules(rules);
  }
  ```

- 热点参数

  热点参数规则（`ParamFlowRule`）类似于流量控制规则（`FlowRule`）：

  |       属性        | 说明                                                         | 默认值   |
  | :---------------: | :----------------------------------------------------------- | :------- |
  |     resource      | 资源名，必填                                                 |          |
  |       count       | 限流阈值，必填                                               |          |
  |       grade       | 限流模式                                                     | QPS 模式 |
  |   durationInSec   | 统计窗口时间长度（单位为秒），1.6.0 版本开始支持             | 1s       |
  |  controlBehavior  | 流控效果（支持快速失败和匀速排队模式），1.6.0 版本开始支持   | 快速失败 |
  | maxQueueingTimeMs | 最大排队等待时长（仅在匀速排队模式生效），1.6.0 版本开始支持 | 0ms      |
  |     paramIdx      | 热点参数的索引，必填，对应 `SphU.entry(xxx, args)` 中的参数索引位置 |          |
  | paramFlowItemList | 参数例外项，可以针对指定的参数值单独设置限流阈值，不受前面 `count` 阈值的限制。**仅支持基本类型和字符串类型** |          |
  |    clusterMode    | 是否是集群参数流控规则                                       | `false`  |
  |   clusterConfig   | 集群流控相关配置                                             |          |

  ```java
  ParamFlowRule rule = new ParamFlowRule(resourceName)
      .setParamIdx(0)
      .setCount(5);
  // 针对 int 类型的参数 PARAM_B，单独设置限流 QPS 阈值为 10，而不是全局的阈值 5.
  ParamFlowItem item = new ParamFlowItem().setObject(String.valueOf(PARAM_B))
      .setClassType(int.class.getName())
      .setCount(10);
  rule.setParamFlowItemList(Collections.singletonList(item));
  
  ParamFlowRuleManager.loadRules(Collections.singletonList(rule));
  ```

- 访问控制

  授权规则，即黑白名单规则（`AuthorityRule`）非常简单，主要有以下配置项：

  - `resource`：资源名，即规则的作用对象
  - `limitApp`：对应的黑名单/白名单，不同 origin 用 `,` 分隔，如 `appA,appB`
  - `strategy`：限制模式，`AUTHORITY_WHITE` 为白名单模式，`AUTHORITY_BLACK` 为黑名单模式，默认为白名单模式

  ```java
  AuthorityRule rule = new AuthorityRule();
  rule.setResource("test");
  rule.setStrategy(RuleConstant.AUTHORITY_WHITE);
  rule.setLimitApp("appA,appB");
  AuthorityRuleManager.loadRules(Collections.singletonList(rule));
  ```

## 整合控制台

就在之前的项目中，在已有依赖的基础上添加下述的依赖，

```xml
<!--整合sentinel控制台-->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-transport-simple-http</artifactId>
    <version>1.8.1</version>
</dependency>
```

![](https://mudongjing.github.io/gallery/cloud/alibaba/sentinel/vmopt.png)

无需对代码做修改，在项目启动选项的`VM options`中指定我们启动的sentinel对应的ip和端口，例如

```shell
-Dcsp.sentinel.dashboard.server=127.0.0.1:8110
```

启动项目后，一般需要先访问自己项目的一个API，之后，等一会，就可以在sentinel的页面中看到自己的资源信息。

![](https://mudongjing.github.io/gallery/cloud/alibaba/sentinel/dash.png)

也可以在对应的规则下看到我们代码给定的设置，

![](https://mudongjing.github.io/gallery/cloud/alibaba/sentinel/flow.png)

点击编辑，就可以修改对应的规则，

![](https://mudongjing.github.io/gallery/cloud/alibaba/sentinel/edit-flow.png)

## 整合alibaba-cloud

之前我们添加的所有关于sentinel的依赖都可以忽略，只需要下面的

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

基本的java代码仍然可以使用之前的那些，稍微需要改变的就是，可以在`application.yml`文件中指定sentinel的ip和端口，

```yaml
server:
  port: #随便写一个自己服务用的端口

spring:
  application:
    name: sentinel-service #随便写个服务名
  cloud:
    sentinel:
      transport:
        dashboard: ip:端口#控制台地址
```



# 附录

## 配置项

下面均摘自官网。

- 基础

  | 名称                                   | 含义                                                     | 类型     | 默认值                | 是否必需 | 备注                                                         |
  | -------------------------------------- | -------------------------------------------------------- | -------- | --------------------- | -------- | ------------------------------------------------------------ |
  | `project.name`                         | 指定应用的名称                                           | `String` | `null`                | 否       |                                                              |
  | `csp.sentinel.app.type`                | 指定应用的类型                                           | `int`    | 0 (`APP_TYPE_COMMON`) | 否       | 1.6.0 引入                                                   |
  | `csp.sentinel.metric.file.single.size` | 单个监控日志文件的大小                                   | `long`   | 52428800 (50MB)       | 否       |                                                              |
  | `csp.sentinel.metric.file.total.count` | 监控日志文件的总数上限                                   | `int`    | 6                     | 否       |                                                              |
  | `csp.sentinel.statistic.max.rt`        | 最大的有效响应时长（ms），超出此值则按照此值记录         | `int`    | 4900                  | 否       | 1.4.1 引入                                                   |
  | `csp.sentinel.spi.classloader`         | SPI 加载时使用的 ClassLoader，默认为给定类的 ClassLoader | `String` | `default`             | 否       | 若配置 `context` 则使用 thread context ClassLoader。1.7.0 引入 |

  其中 `project.name` 项用于指定应用名（appName）。若未指定，则默认解析 main 函数的类名作为应用名。**实际项目使用中建议手动指定应用名**。

- 日志

  | 名称                           | 含义                                                         | 类型      | 默认值                   | 是否必需 | 备注       |
  | ------------------------------ | ------------------------------------------------------------ | --------- | ------------------------ | -------- | ---------- |
  | `csp.sentinel.log.dir`         | Sentinel 日志文件目录                                        | `String`  | `${user.home}/logs/csp/` | 否       | 1.3.0 引入 |
  | `csp.sentinel.log.use.pid`     | 日志文件名中是否加入进程号，用于单机部署多个应用的情况       | `boolean` | `false`                  | 否       | 1.3.0 引入 |
  | `csp.sentinel.log.output.type` | Record 日志输出的类型，`file` 代表输出至文件，`console` 代表输出至终端 | `String`  | `file`                   | 否       | 1.6.2 引入 |

  > **注意**：若需要在单台机器上运行相同服务的多个实例，则需要加入 `-Dcsp.sentinel.log.use.pid=true` 来保证不同实例日志的独立性。

- 传输

  | 名称                                 | 含义                                                         | 类型     | 默认值 | 是否必需                                                     |
  | ------------------------------------ | ------------------------------------------------------------ | -------- | ------ | ------------------------------------------------------------ |
  | `csp.sentinel.dashboard.server`      | 控制台的地址，指定控制台后客户端会自动向该地址发送心跳包。地址格式为：`hostIp:port` | `String` | `null` | 是                                                           |
  | `csp.sentinel.heartbeat.interval.ms` | 心跳包发送周期，单位毫秒                                     | `long`   | `null` | 非必需，若不进行配置，则会从相应的 `HeartbeatSender` 中提取默认值 |
  | `csp.sentinel.api.port`              | 本地启动 HTTP API Server 的端口号                            | `int`    | 8719   | 否                                                           |
  | `csp.sentinel.heartbeat.client.ip`   | 指定心跳包中本机的 IP                                        | `String` | -      | 若不指定则通过 `HostNameUtil` 解析；该配置项多用于多网卡环境 |

  > 注：`csp.sentinel.api.port` 可不提供，默认为 8719，若端口冲突会自动向下探测可用的端口。

- 控制台配置项

  | 配置项                                        | 类型    | 默认值                    | 最小值 | 描述                                                         |
  | --------------------------------------------- | ------- | ------------------------- | ------ | ------------------------------------------------------------ |
  | auth.enabled                                  | boolean | true                      | -      | 是否开启登录鉴权，仅用于日常测试，生产上不建议关闭           |
  | sentinel.dashboard.auth.username              | String  | sentinel                  | -      | 登录控制台的用户名，默认为 `sentinel`                        |
  | sentinel.dashboard.auth.password              | String  | sentinel                  | -      | 登录控制台的密码，默认为 `sentinel`                          |
  | sentinel.dashboard.app.hideAppNoMachineMillis | Integer | 0                         | 60000  | 是否隐藏无健康节点的应用，距离最近一次主机心跳时间的毫秒数，默认关闭 |
  | sentinel.dashboard.removeAppNoMachineMillis   | Integer | 0                         | 120000 | 是否自动删除无健康节点的应用，距离最近一次其下节点的心跳时间毫秒数，默认关闭 |
  | sentinel.dashboard.unhealthyMachineMillis     | Integer | 60000                     | 30000  | 主机失联判定，不可关闭                                       |
  | sentinel.dashboard.autoRemoveMachineMillis    | Integer | 0                         | 300000 | 距离最近心跳时间超过指定时间是否自动删除失联节点，默认关闭   |
  | sentinel.dashboard.unhealthyMachineMillis     | Integer | 60000                     | 30000  | 主机失联判定，不可关闭                                       |
  | server.servlet.session.cookie.name            | String  | sentinel_dashboard_cookie | -      | 控制台应用的 cookie 名称，可单独设置避免同一域名下 cookie 名冲突 |

## API 指令

### 流量

- 统计

  流量控制主要有两种统计类型，一种是统计并发线程数，另外一种则是统计 QPS。类型由 `FlowRule` 的 `grade` 字段来定义。其中，0 代表根据并发数量来限流，1 代表根据 QPS 来进行流量控制。其中线程数、QPS 值，都是由 `StatisticSlot` 实时统计获取的。

  我们可以通过下面的命令查看实时统计信息：

  ```shell
  curl http://localhost:8719/cnode?id=resourceName
  ```

  输出内容格式如下：

  ```
  idx id     thread  pass  blocked   success  total Rt   1m-pass   1m-block   1m-all   exception
  2   abc647    0     46      0         46      46   1     2763       0         2763     0
  ```

  其中：

  - thread： 代表当前处理该资源的并发数；
  - pass： 代表一秒内到来到的请求；
  - blocked： 代表一秒内被流量控制的请求数量；
  - success： 代表一秒内成功处理完的请求；
  - total： 代表到一秒内到来的请求以及被阻止的请求总和；
  - RT： 代表一秒内该资源的平均响应时间；
  - 1m-pass： 则是一分钟内到来的请求；
  - 1m-block： 则是一分钟内被阻止的请求；
  - 1m-all： 则是一分钟内到来的请求和被阻止的请求的总和；
  - exception： 则是一秒内业务本身异常的总和。

- 限流

  