## Spring MVC的主要知识介绍

[TOC]

### 1. 主要的运行机制

spring mvc是作为一个WEB框架使用，主要的作用就是在我们指定的网址上根据不同的页面需求而返回对应的页面信息。

如浏览器中输入地址：`https://我们的主网址/index.html`

1. 这里用户试图获取对应网址下的`index.html`页面
2. 那么当浏览器将这一请求发送过来时，附带着说明我需要这样一个文件
3. 我们的WEB框架就会根据该需求，识别出需要的是一个`index.html`文件，并将本地资源中的`index.html`文件作为回复发送给浏览器，附带着告诉它这是什么类型的文件
4. 接着浏览器就会按照html文件格式，读取文件并渲染显示在页面中。

大体上，Spring MVC需要做的主要工作就是上述的流程，而这种识别工作就交给被标注了@Controller注解的类来完成。

但是我们的页面很多不是那种仅用于读取的静态页面，而是涉及到数据的交换，此时则需要结合数据库工作，目前常用的就是利用Mybatis作为数据库的操作工具，帮助我们存储和读取数据。

---------------------------

而涉及到spring框架时，常看到的就是容器，IOC。

- 首先，我们对数据的操作，这里先默认是常用的关系数据库，内部有表，表又有行。
- 行作为一条记录，包含了该表对一条数据要求的各种元素，如一个学生的表，则要求有学号，性别，年龄等信息。此时学生表中的记录可概括为一个学生类，类中包含了这些信息，而每条记录则是一个实例。
- 当然了，由于我们在java中常用的单例模式，一个类仅创建一个对象就足够了。
- 之后的数据库操作中，我们有一个学生的对象，当用户通过页面提交或需要修改一个学生的信息，那么将这些信息纳入到我们的学生对象中的相应的变量上
- 现在，用户提交的信息就放在一个对象中可以被传递，也可以被Mybatis识别内部对应的信息，并通过相应的语句发送给数据库
- 这样，我们就可以通过一个对象完成一个表中各种信息的传递，如果我们有多个表，只需要创建相应的类，并实例出一个对象即可。
- 那么一个大的工程，我们的对象可能有很多，我们在试图使用一个对象时，是否需要判断是否已经有这样一个对象。
- 在一个大工程中，去查询某一个对象是否均有一个对象是枯燥且较为麻烦的事情。
- 容器就是负责对这些对象进行管理，我们会发现spring中的编程动不动就在一个创建对象的方法上加一个@Bean注解，这就是告诉spring需要产生一个对象，并纳入容器中。
- 之后，就会发现我们再使用对象时，指定声明并加上@Autowired之类的注解即可，此时，spring发现这样一个注解，就会从容器中把对应的对象注入到你声明的对象中，这就是IOC。
- 由此，我们可以借助于容器，方便我们进行信息传递。

上述就是，我们使用Spring MVC主要的目的和重要的技术点。而具体到实际的项目，则需要考虑一些细枝末节，对产品做优化，不至于粗糙。

 ### 2.  代码的主要内容

前面了解了这一框架的用处，那么实际的使用则需要注重功能的组织。

- 首先在创建项目时，需要引入spring-webmvc的依赖。

- 而由于浏览器发送来的请求多种多样，不一定都是简单地获取页面，即使是页面，当我们的目标复杂时，对网址的各种状况也有明确的任务分类，导致有多个Controller对象，因此需要一个请求的管理器，这里是称为中央调度器DispatcherServlet来根据请求选择不同的应对方案，这是一个继承自类HttpServlet的servlet，需要在web.xml注册，即
- 需要的话，在写一些页面文件
- 再写几个控制器类即Controller，以负责处理页面的各种效果
- 为了保证spring知道你的Controller在哪，还需要一个配置文件指明控制器所在的包的位置
- 此外，我们可能使用了一些视图解析器以处理页面效果，同样需要在配置文件中指明对应的包的位置。

### 3. 项目构建

我们可以利用maven或gradle引入`javax.servlet-api`，`spring-webmvc`。

大致的源代码文件框架如下：

```basic
├───java
│   └───自定义的
│       └───包名
│           ├───controller(自己随便命名)
│           └───···
│			└───···
├───resources
│		└───springmvc.xml//因为难受而自定义的另一个配置文件
└───webapp
	└───WEB-INF//这里面的文件对用户是不开放的
		└───web.xml
		└───dispatcher-servlet.xml
			//这个名字是随着web.xml中自己定义的调度器名称而改变（是默认的格式），对应着springmvc的配置
```

首先，整个项目需要中央调度器，要求在web.xml中告诉tomcat之类的服务器生成一个DispatchServlet的对象【更严格的说，tomcat只能算是Servlet容器】

最简单的web.xml指明要加载调度器对象：

```xml-dtd
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                             http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
    <servlet>
		<!--名字随意，也对应着dispatcher-servlet.xml-->
		<servlet-name>dispatcher</servlet-name>
		<!--指明需要生成对象的类的全限定名-->
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
		<load-on-startup>1</load-on-startup><!--加载顺序，这里就是要求赶紧加载，数字越小越急-->
<!--如果觉得dispatcher和dispatcher-servlet.xml必须对应非常难受。-->
		<init-param>
			<param-name>contextConfigLocation</param><!--springmvc配置文件的位置属性-->
			<param-value>classpath:springmvc.xml</param-value>
			<!--指定的位置，这里是在resources目录下-->
		</init-param>
	</servlet>
</web-app>

```

上述完成后，基本上搭配tomcat就能启动了。但是作为一个调度器，自然需要添加一些不同的情况，如

```java
<servlet-mapping>
        <servlet-name>调度器的名字</servlet-name>
        <url-pattern>*.随意的扩展名</url-pattern>
     	<!--当传输来的url需求对应着不同的扩展名，将会对应着不同的操作-->
 </servlet-mapping>
     <!--如果我们自己网站的域名是www.bilibili.com-->
     <!--那么www.bilibili.com/v/music对应的是音乐区，www.bilibili.com/anime对应的是番剧区-->
     <!--此时，不同的url在功能上有着巨大的差别，因此，也可以通过指定域名之后的名称确定具体的职责，以指定对应的类来负责-->
 <servlet-mapping>
        <servlet-name>调度器的名字</servlet-name>
        <url-pattern>/anime</url-pattern>
    	<!之后"www.bilibili.com/anime..."对应的url将由番剧区的类负责解析并回复-->
 </servlet-mapping>
```

上述代码同样添加在web.xml文件中，所谓的扩展名即index.jsp，index.html属于不同的文件。

### 3. @Controller

> @Controller本身于@Bean类似，都是自发地实例化一个类，并将对象放入Spring容器中。
>
> 在底层中，容器实际上是一个Map对象，负责存储所有的对象，以及调用。当产生对象后，便使用map.put("新对象名"，实际的对象)。

上述过程基本完成了一些初步的没什么太特殊的工作。接下来，我们需要考虑，一开始我们认为的WEB框架需要做的工作，就是根据url做相应的工作。

@Controller注解，是告诉spring我是一个控制类，当url进来后，可以通过我来进行判断工作含义并执行工作。

我们首先定义一个类，并标注Controller注解，然后在内部加上不同的方法，方法内部是我们需要返回怎样的页面文件，或其它各种结果，以及进行其它额外的操作。

仅仅需要在内部的方法上加上@RequestMapping(value="/地址，如bilibili的anime")，这个注解也可以放在类的上面，总之就是指示接下来的内容适应的url类型，大致的样子如下：

```java
@RequestMapping(value="/")
public class MyController(){
    @RequestMapping(value="anime")
    public 类型 dosomething(){
        //发现当前对应的是地址"www.bilibili.com/anime..."
        //可以随便做点事
    }
}
```

虽然完成了控制器，但spring此时是不知道它的存在的，需要在前面文件树中的`springmvc.xml`或`dispatcher-servlet.xml`文件中放入控制器类所在的包，spring就会知道把这里扫描一遍。

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:tx="http://www.springframework.org/schema/tx" xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc" xmlns:p="http://www.springframework.org/schema/p"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx.xsd
       http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd">
<context:component-scan base-package="包的路径"></context:component-scan>
</beans>
```

如果我们在控制器中执行了相关的命令，并且试图返回一个页面时，

​	*例如方法的类型为ModelAndView时，可以添加各种数据进去，而页面文件本身可以放在其它位置，使用setViewName()可以指向该页面文件的位置*

​	*但有时候，我们是将大量这类页面文件放在同一目录下，或者所处的目录是处于同一目录下的，而且文件类型相同。那么，我们就希望把前面和后面总是重复的内容删减掉，为了达到这一一点，可以在Springmvc的配置文件中指定这些返回页面位置的前缀和后缀，以后就只需要写对应的文件名即可*

```xml
<bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
       <property name="prefix" value="路径前缀"/><!--注意:这个前缀前后都需要/符号，以表明是目录-->
    <!--额外的，前面提到WEB-INF目录下的文件是对用户隐藏的，如果调用的页面不希望用户能够直接调用，则可以在		WEB-INF目录下专门开辟一块目录存放页面文件-->
       <property name="suffix" value=".文件类型"/>
</bean>
```





