## SpringBoot 基本使用

### 1. 简单配置【mybatis】

前面的mybatis文档中，我们简单介绍了mybatis如何在spring环境中配置完成工作。但是还是有大量的配置工作需要我们自己完成。springboot就可以极大地减轻我们这种无聊的配置操作。

我们这里都是使用IDEA为工具的，创建项目的时候，可以使用其中的springboot 的初始化选项，方便指定常用的依赖。

大致的依赖如下，【其它一些额外的依赖，后面在添加】

```xml
    <artifactId>spring-boot-starter-data-jdbc</artifactId>
    <artifactId>spring-boot-starter-web</artifactId>
    <artifactId>mysql-connector-java</artifactId>
    	<version>5.1.47</version><!-- 正常情况下，默认的应该是8.0+的版本，但我本机的mysql有点老-->
    <artifactId>spring-boot-starter-test</artifactId>
    	<scope>test</scope>
    <artifactId>druid</artifactId><!--数据库连接池,阿里巴巴的-->
    	<version>1.2.6</version>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    	<version>2.1.4</version>
    <artifactId>spring-boot-starter-thymeleaf</artifactId><!--取代的jsp的模板引擎-->
```

然后是，配置一些属性了，正常用idea的springboot创建了项目后，resources目录下有一个`application.properties`文件，可以在其中写入指定的属性值，但是为了更好看一些，建议改成`.yml`后缀的。

下面是大概的内容，

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.jdbc.Driver#8.0+的需要中间的cj
    type: com.alibaba.druid.pool.DruidDataSource//连接池
    username: 用户名
    password: 密码
    url: jdbc:mysql://localhost:3306/数据库名?characterEncoding=utf-8
    #8.0+的还需要后面加上&serverTimezone=GMT 用来表示时区
server://对应的web页面地址
  port: 8080
  address: localhost

mybatis:
  type-aliases-package: dao层或mapper接口的包
  mapper-locations: classpath: resources目录下mapper对应的xml文件的ant格式，例如mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true #启用驼峰命名机制，不需要可以不设置
```

OK！

以前花里胡哨的各种方法配置，到这里就基本结束了，springboot回自动读取这个配置文件，自动完成对应的设置。

```java
@SpringBootApplication
@MapperScan(basePackages="对应的mapper接口或dao层的包")//spring忽略@Mapper，需要mybatis主动扫描
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

到此需要的配置就完成了。

### 2. 简单实现

大致的文件体系如下，

```basic
├───java
│   └───com
│       └───example
│           └───demo
│               │   DemoApplication.java #启动类
│               ├───controller
│               │       UserController.java
│               ├───dao 
│               │       UserMapper.java#对应的mapper接口
│               ├───pojo 
│               │       User.java #对应表的类
│               └───service
│                   │   UserService.java
│                   └───Impl
│                           UserServiceImpl.java
└───resources
    │   application.yml
    ├───mapperXml
    │       UserMapper.xml
	├───static #用于存放js,css,图片等静态文件
    └───templates
			index.html #web门户页面
```

首先假设我们的表`user_table`内部的结构包含 `id`, `user_id`, `user_name`.

```java
@Data//隐性生成settter/getter方法，和其它object固有的方法
@AllArgsConstructor//隐性生成带参数的构造方法
@NoArgsConstructor//无参构造方法
//以上都是lombok中的注解方法，需要引入对应的依赖，IDEA还必须安装对应的插件
@TypeAlias("USER")//定义一个别名，可在之后的mapper的xml文件中使用
public class User {
    private Integer id;
    private Integer userId;//因为驼峰命名法，对应user_id
    private String userName;
}
```

```java
@Mapper//用于mybatis扫描并生成指定的bean方便之后的注入，
//但是在IDEA中编写的时候，因为它发现不了对应的bean，会提示你有问题，可以忽略，
//有的人会加上@Repository以明确说明是一个bean，但是不需要
//另外，【我自己的血泪，不要试着直接在通过编写测试方法测试mapper接口，只会返回失败】，
//只能启动项目，由mybatis生成bean才行
public interface UserMapper {
    User selectUser(String name);
}
```

```xml
<mapper namespace="com.example.demo.dao.UserMapper">
    <select id="selectUser" parameterType="string" resultType="USER"><!--这里使用了别名-->
        select * from user_table where user_name=#{name}
    </select>
</mapper>
```

```java
public interface UserService {
    User selectUser(String name);//这里可以随便写方法名，这里为了方便就弄个一样的
}
```

```java
@Service
public class UserServiceImpl implements UserService {
    private UserMapper userMapper;//本来可以直接在上面放上@Autowired，但spring3之后觉得不好
    @Autowired//于是，这里我们在这里用了set注入
    public void setUserMapper(UserMapper userMapper){
        this.userMapper=userMapper;
    }
    @Override
    public User selectUser(String name) {
        User user=new User();
        user=userMapper.selectUser(name);
        return user;
    }
}
```

```java
@Controller
public class UserController {
    @Autowired//这里就没用set注入，一样有用，只是一直提醒你不推荐
    private UserService userService;
    @GetMapping("/json/{name}")
    @ResponseBody//为了方便，直接显示为json格式，需要额外配置json的依赖包，可以使用谷歌的gson
    public User json(@PathVariable String name){
        User user= new User();
        user=userService.selectUser(name);
        return user;
    }
}
```

通过上面的实现，我们会发现，虽然换了springboot，但只是配置方便了一些，具体的实现还是原先那一套。

读者可以在目录 `springBoot-Mybatis` 下看一看较为丰富的实现，主要是页面的实现，但并不是本文的主要讲解内容。

