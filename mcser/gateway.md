# 使用

创建一个module，因为后面我们也需要使用nacos进行操作，需要nacos的依赖，

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<!-- 如果暂时不需要nacos，先不要引入nacos的依赖，有可能会出现问题-->
<!-- 因为一旦引入了该依赖，他就可能会自发地想活动，当没有对应的配置则报异常-->
<!-- 也可以使用spring.cloud.nacos.discovery.enabled=false 迫使依赖不活动-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

该module只是简单的实现gateway功能，实际的java的代码基本没有，首先按惯例创建一个启动类即可。

之后需要在配置文件中进行主要的操作，

> 假设我们有一个负责订单的API，希望网关负责转发，那么我们就只给用户提供网关的API，用户通过指定的格式发送请求订单服务的url，网关负责审查url的格式是哪一种，如果是符合订单请求的格式，则将url的对应的内容重新转发给订单的API。
>
> 由此，我们就至少需要设置以下几个内容：
>
> > 1. 网关自己的服务ip和端口
> > 2. 订单服务API的地址
> > 3. 对应服务的url格式
> > 4. 如何提取一开始的url中对应的内容

- 无nacos

  ```yaml
  #假设对应的订单服务API是http://127.0.0.1:8081/order/add/{id}
  #意为添加对应id的订单
  server:
    port: 9000 #网关对应的端口
  spring:
    application:
      name: api-gateway
    cloud:
      gateway:
        routes: #可以设置多个路由规则
          - id: order_route #随便定义一个名字
            uri: http://127.0.0.1:8081 #指定需要转发的地址
            predicates: # 称之为 断言
              - Path=/order_ser/order/** #用来匹配地址的前缀,默认是将对应的/order/**放到uri之后
            filters: #为了过滤掉重新转发的地址中的不需要的内容，如之前的order_ser
              - StripPrefix=1 # 表示去掉第一层的那个内容，即/order_ser
              	#实际上，具体的操作也不知道是智能还是一根筋
              	#过滤/order_ser，只要uri对应的第一层是/order_ser就会被去掉，
              	#即使是我们给定的uri的第一层
              	#也就是，加入我们给定的uri是http://127.0.0.1:8081/order_ser
              	#其它配置不变，这个/order_ser一样会被去掉，也就是不厌其烦地过滤了两次
              	#知道第一层不是/order_ser
        httpclient:
          connect-timeout: 10000
          response-timeout: 5s
  ```

  当我们访问`http://localhost:9000/order_ser/order/add/2`，则会在服务端内部跳转到`http://localhost:8081/order/add/2`

- 依靠nacos

  ```yaml
  #假设对应的订单服务已经在nacos中注册了服务发现，名字是order-service
  #则下面需要配置对应服务的同命名空间和组，否则无法获取对应的服务
  #其它的，除了修改一下uri，可以不做其它改动
  server:
    port: 9000
  spring:
    application:
      name: api-gateway
    cloud:
      gateway:
        routes: #可以设置多个路由规则
          - id: order_route #随便定义一个名字
            uri: lb://order-service #指定对应的订单发服务名，lb即loadBanlance
            predicates:
              - Path=/order_ser/order/** 
            filters: 
              - StripPrefix=1 
        httpclient:
          connect-timeout: 10000
          response-timeout: 5s
      nacos:
        discovery:
          server-addr: #acosip:地址
          username: 
          password: 
          namespace: #order-service同命名空间
          group: #order-service同组
  ```
  
  - 全局服务发现
  
    上述的配置中，我们可以指定服务名进行转发，但是如果否需要转发的服务特别多，可以让gateway获取nacos对应位置上存在的所有服务，并自动根据服务名进行转发
  
    ```yaml
    server:
      port: 9000
    spring:
      application:
        name: api-gateway
      cloud:
        gateway:
          discovery:
            locator:
              enabled: true #基于服务发现的路由规则
              lower-case-service-id: true #服务名均转化为对应的小写
          httpclient:
            connect-timeout: 10000
            response-timeout: 5s
        nacos:
          discovery:
            server-addr: 
            username: 
            password: 
            namespace: 
            group: 
    ```
  
    > 例如order订单服务名为order-service，在nacos通过负载均衡可以使`http://order-servce/order/add/{id}`转化为对应的机器上的order服务。
    >
    > 那么上面的操作后，我们可以访问`http://127.0.0.1:9000/order-service/order/add/{id}`，则会转化为对应的`http://order-service/order/add/{id}`

# 结合sentinel

对于sentinel结合spring cloud gateway，在sentinel的源码中由对应的示例代码，位于`sentinel-demo/sentinel-demo-spring-cloud-gateway` 。

> 首先启动自己的sentinel的jar包，做好准备。

- 不依赖spring-cloud-alibaba体系

  如果我们使用spring-cloud-alibaba整合的sentinel和gateway的一些依赖，可以节省一些工作量。

  但同样的，作为一个微服务的组件，最基本的就是它应该能够单独拿出来使用，在相对独立的情况下，使用的主要依赖是

  ```xml
  <!-- 基本的gateway-->
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-gateway</artifactId>
  </dependency>
  <!-- 作为注册中心，也可以换成其它的-->
  <dependency>
      <groupId>com.alibaba.cloud</groupId>
      <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
  </dependency>
  <!--这是alibaba提供的，用于我们整合sentinel和gateway的-->
  <dependency>
      <groupId>com.alibaba.csp</groupId>
      <artifactId>sentinel-spring-cloud-gateway-adapter</artifactId>
      <version>1.8.2</version>
  </dependency>
  <!--用于对接sentinel控制台的-->
  <dependency>
      <groupId>com.alibaba.csp</groupId>
      <artifactId>sentinel-transport-simple-http</artifactId>
  </dependency>
  ```

  照例，先写一个常规的启动类。

  然后，主要写一下对应的配置文件

  > 假设，nacos中已经注册了一个order-service的订单服务，除了前面一致提及的增加订单的API，`order/add/{id}`，我们还可以有一个减订单的API，`order/reduct/{id}`。
  >
  > 我们可以要求对所有API限流，但也可以有针对性的限流。

  ```yaml
  server:
    port: 9000
  spring:
    application:
      name: api-gateway
    cloud:
      sentinel:
        filter:
          enabled: false # 避免当前的网关被sentinel监管
      gateway:
        discovery:
          locator:
            enabled: true #基于服务发现的路由规则
            lower-case-service-id: true #服务名均转化为对应的小写
        httpclient:
          connect-timeout: 10000
          response-timeout: 5s
        # 上面的是全局服务发现的转发操作
        # 下面是我们之前介绍的普通方式，二者目的相同，但实际我们进行uri填写时有所不同
        # 而且，这两种状况是共存的
        routes:
          - id: order-service-xx #id可以随便写
            uri: lb://order-service
            predicates:
              - Path=/order/add/**,/order/reduct/** 
              		#多种API的匹配模式，这里当然可以合并为/order/**
      nacos:
        discovery:
          server-addr: 
          username: 
          password: 
          namespace: 
          group: 
  ```

  - 纯粹的限流

    这里的纯粹就是，配置内容中，出现的匹配模式都会收到sentinel的监管，可以触发对应的限流条件

    为实现这一点，需要我们实现一个方法，

    ```java
    @Configuration
    public class GatewayConfiguration {
    //*****************这一部分都是较为固定的，复制，基本就ok**********
    //*																	*
    //*																	*
        private final List<ViewResolver> viewResolvers;
        private final ServerCodecConfigurer serverCodecConfigurer;
    
        public GatewayConfiguration(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                                    ServerCodecConfigurer serverCodecConfigurer) {
            this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
            this.serverCodecConfigurer = serverCodecConfigurer;
        }
    
        //触发限流将启动下面的bean对象
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)//优先级
        public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
            // Register the block exception handler for Spring Cloud Gateway.
            return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
        }
    
        @Bean
        @Order(-1)
        public GlobalFilter sentinelGatewayFilter() {
            return new SentinelGatewayFilter();
        }
    //*																	*
    //*																	*
    //*****************这一部分都是较为固定的，复制，基本就ok**********
        
        @PostConstruct//该类作为spring的一个容器，该注解表示项目启动时，该方法也会一起启动
        public void doInit() {
            initGatewayRules();
            //initBlockHandler(); //如果希望使用自己写的返回格式，再解除注释
        }
    
        //限流规则
        private void initGatewayRules() {
            Set<GatewayFlowRule> rules = new HashSet<>();
            rules.add(new GatewayFlowRule("order-service-xx")//与配置文件中对应的id保持一致
                    .setCount(2)// 限流阈值
                    .setIntervalSec(10));//统计时间窗口，单位 秒，这里设定是10秒
            // 这里的规则就是10秒内访问超过两次，就触发限流
            //并且要求我们访问的uri必须是对应·order-service-xx·的断言
            //即http://localhost:9000/order/add/{id}的情形
            GatewayRuleManager.loadRules(rules);
        }
    
        //自定义限流返回的内容，这一部分可写可不写，主要告诉读者有这种操作
        /*
        private void initBlockHandler(){
            BlockRequestHandler blockRequestHandler=new BlockRequestHandler() {
                @Override
                public Mono<ServerResponse> handleRequest(ServerWebExchange serverWebExchange, Throwable throwable) {
                    Map<String, String> result = new HashMap<>();
                    //里面的内容，读者可以随便写
                    result.put("原因", "访问次数过多而触发限流");
                    result.put("code", String.valueOf(HttpStatus.TOO_MANY_REQUESTS.value()));
                    result.put("message", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
                    result.put("路由", "order-service");
                    return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(result));
                }
            };
            GatewayCallbackManager.setBlockHandler(blockRequestHandler);
        }
        */
    }
    ```

  - 部分限流

    如果我们只是希望几个关键的API能够限流，其它的则无所谓，则可以进行分组限流，

    ```java
    @Configuration
    public class GatewayConfiguration {
        private final List<ViewResolver> viewResolvers;
        private final ServerCodecConfigurer serverCodecConfigurer;
    
        public GatewayConfiguration(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                                   ServerCodecConfigurer serverCodecConfigurer) {
            this.viewResolvers=viewResolversProvider.getIfAvailable(Collections::emptyList);
            this.serverCodecConfigurer = serverCodecConfigurer;
        }
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
         return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
        }
        @Bean
        @Order(-1)
        public GlobalFilter sentinelGatewayFilter() {
            return new SentinelGatewayFilter();
        }
    //********************************
        @PostConstruct
        public void doInit() {
            initCustomizedApis();
            initGatewayRules();
        }
    
        //限流规则
        private void initGatewayRules() {
            Set<GatewayFlowRule> rules = new HashSet<>();
            rules.add(new GatewayFlowRule("order-api")//这里不再对应配置文件中的id，
                      							//而是随便，作为一个限流规则的id
                    .setCount(2)// 限流阈值
                    .setIntervalSec(10));//统计时间窗口，单位 秒，这里设定是10秒
            GatewayRuleManager.loadRules(rules);
            initCustomizedApis();//这个就是完成我们指定uri的限流
            //除此，上面可以写多个不同的规则，只要命名好对应的规则id
            //initCustomizedApis()内部也可以命名对应规则的限流分组
        }
        //限流分组
        private void initCustomizedApis() {
            Set<ApiDefinition> definitions = new HashSet<>();
            ApiDefinition api1 = new ApiDefinition("order-api")//这个就需要对应上面限流规则id
                    .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                        add(new ApiPathPredicateItem().
                            setPattern("/order-service/order/reduct/1"));
                        add(new ApiPathPredicateItem().setPattern("/order/add/1"));
                        //上述，我们指定了两种访问方式，
                        //实际，配置的格式就是普通的匹配模式，
                        //可以使用/order/**之类的
                    }});
            definitions.add(api1);
            GatewayApiDefinitionManager.loadApiDefinitions(definitions);
        }
    }
    ```

    



# 自定义断言



# 自定义过滤器





# 附录

## 现有的一些断言



## 现有的一些过滤器

