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

# 结合sentinel





# 自定义断言



# 自定义过滤器





# 附录

## 现有的一些断言



## 现有的一些过滤器

