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

> 其实，sentinel可以不去学习的那么深入，相比于以往的很多组件我，主要需要使用代码进行设置，sentinel很多功能都可以在其控制台的页面中进行操作。

- 仅使用对应的依赖

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
  
  <!--整合sentinel控制台-->
  <dependency>
      <groupId>com.alibaba.csp</groupId>
      <artifactId>sentinel-transport-simple-http</artifactId>
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

  此时，运行启动类，打开http://localhost:端口/hello，就可以看到返回"hello world"字样，当我们请求的快一点，就会触发降级，并返回"被流控"的字样。













- sentinel

配置了sentinel-transport-simple-http依赖，就相当于一个客户端，在idead VM选项中配置对应的sentinel地址



流控模式

> 链路控制：web-context--unify:false #默认链路收敛，我们需要设置为true,
>
> 热点参数：需要在对应的方法上设置SentinelResource注解。同样是，指定一个value对应的名字作为资源名，还有一个是handler。



为解决的问题，如何让sentinel中的配置，自动更新到nacos中













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

