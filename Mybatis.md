## Mybatis基本使用

> Mybatis其实就是一个方便我们操作数据库的工具，使用它，我们可以方便地存储和查询数据。
>
> 它是一种ORM框架（ORM即Obejct/Relation Mapping，对象/关系数据库映射）
>
> 官方也有[网站]([mybatis – MyBatis 3 | 入门](https://mybatis.org/mybatis-3/zh/getting-started.html))专门介绍了其使用方法，不过在这里我们不会单纯介绍mybatis如何使用，而是简单介绍[mybatis-spring]([mybatis-spring –](http://mybatis.org/spring/zh/getting-started.html))与spring的使用方法。

==本文使用的mybatis版本为3.5.7==

### 1. 预备操作

为了操作数据库，我们首先需要利用JDBC与数据库建立联系，再通过给定的方法传入sql语句，借着进行数据录入或结果读取。总之，这一系列的操作麻烦而且缺乏灵活性，更重要的是本身非常死板。

我们只需要将基本的信息，如数据库地址，用户名，密码等必要的信息传入，即可创建数据库连接。之后，我们就需要设计映射语句，使用方法调用对应的sql语句模板，完成对应的操作。

因此，看起来框架很复杂，但终究就是两个程序，创建连接和写sql语句。

但首先，本文假设读者对于这些数据库操作非常陌生，那么我们首先需要创建一个数据库进行操作。

==这里我们使用mysql数据库==

首先引入mysql的依赖，`mysql-connector-java`，本文是注重与Spring的结合，因此我们需要添加依赖`mybatis-spring`。

> 当然，如果是使用了springboot的读者，可能会知道还有专门的`mybatis-spring`可以结合到spring中使用。

为了建立数据库，读者需要先在本地安装mysql软件，具体的步骤这里不赘述了，网上有大量的教程。

> 当然你也可以使用远程数据库，只要你愿意。【比如可以在阿里云上购买远程数据库服务器，但何必呢，我们现在知识学习】

- 首先登录自己的数据库，本地的话，估计就是

  ```bash
  mysql -u 用户名 -p 密码 #但是不建议将密码这样写在命令中，可以不写，回车后，自然会提醒填写密码
  #首先创建我们这里的专用数据库
  create database mybatis_test;#注意，sql语句都需要分号结尾
  ```

- 为了创建较为完整的表，我们可以先将具体的语句写入sql文件中，再进行调用

  ```sql
  -- sql文件中用 "--"来做注释，主要要留有一个空格，但我们用 "#"也没问题
  -- sql是不区分大小写的，因此下面的大写部分也可以用小写代替
  #初始table
  CREATE TABLE `USER_TABLE` (#``叫重音符，这里其实不用加，但只是作为介绍搞出来
      -- 简单来说，如果是简单的没有声明意义的英文就可以不用重音符
      -- 但是，对于属于sql关键词的，或是中文等其它语言的，则需要标上重音符，以防报错
    `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
      -- AUTO_INCREMENT表示自增，我们每次录入记录都会自动+1
    -- COMMENT没有实际作用，但是可以表示当前这个数据的意义，就像git命令中的commit指定这次提交内容的意义
      -- 可以使用 show full collumns from 表名; 来查看表中各数据对应的comment
    `user_id` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '用户id',
      -- DEFAULT就是简单的设置默认值
      -- 而这些类型后面跟着的括号和里面的数字，表示的是作为字符串它们能够显示出的最小位数
      -- 比如我们设置int(2),那么数字显示出来怎么也得是两个数字的，但是实际上我们发现没有变化
      -- 因为需要额外设置列名是由0填充才会显示出来
      -- 大致的语句为 
      -- ALTER TABLE 表名 MODIFY 列名 数据类型(指定的最小位数) zerofill;
      -- 此时，当指定列名的记录，假如我们设置最小位数为3，而实际数字是1，那么这时候显示出来的就是001
      -- 实际中，我们基本也不会关心这种情况，因此无需太过关心
    `user_name` character (25)  NOT NULL DEFAULT '0' COMMENT '用户姓名',
    PRIMARY KEY (`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
  
  #初始table
  CREATE TABLE `STUDENT_TABLE` (
    `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `student_id` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '学生id',
    `student_school` character (25)  NOT NULL DEFAULT '0' COMMENT '学校',
    PRIMARY KEY (`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8
  ```

  写好sql文件后，保存到本地，复制好地址，进入mysql中，找到对应的数据库，进行调用

  ```sql
  source sql文件地址; 
  #如果事先在sql文件中写入了use 指定的数据库，就不用我们自己进入对应的数据库了
  #总之sql文件内就是一大组sql命令的集合，怎么写，数据库就按照顺序怎么执行
  ```

  > 另外，我们的表中，列名大多是以"名字\_属性"的格式命名，实际上是因为Mybatis中有自动的驼峰命名转换，例如将user_id转为对象属性的userId。
  >
  > 如果不喜欢，也没问题。

### 2. Mybatis准备工作

在上述工作后，设置了依赖，和数据库的构建。

利用mybatis-spring，需要创建一个`SqlSessionFactory`实例连接数据库，另外`SqlSessionTemplate`用于创建对应的映射语句。

为了使用spring的功能，我们需要先添加spring的一些依赖，如spring-aop，spring-context，spring-jdbc，spring-beans，spring-core等。

我们先给出一个大概的文件结构，

```basic
─main
    ├─java
    │  └─org
    │      └─example
    │          │  app.java
    │          ├─config
    │          │  ├─mvc
	│          │  │   
    │          │  ├─mybatis
    │          │  │      MybatisConfig.java
    │          │  └─spring
    │          │          AppConfig.java
    │          │
    │          ├─dao
    │          └─entity
    │                  User.java
    ├─resources
    │  │  mybatis-config.xml
    │  │
    │  └─property
    │       mysql.properties
    └─webapp
        │  index.jsp
        └─WEB-INF
```

其中对于数据库连接，我们将需要的信息放入对应的properties文件中，如下面的mysql.properties，

```properties
jdbc.driver = com.mysql.cj.jdbc.Driver
#这是对应的8.0版本，如果小于8.0，就去掉cj
jdbc.url = jdbc:mysql://localhost:3306/mybatis_test?characterEncoding=UTF-8
jdbc.username = 用户名
jdbc.password = 密码
```

对应的mybatis配置使用下方的类，

【这里使用了java配置，利用了mybatis-spring和spring配合完成了mybatis的配置】

```java
@Configuration
@ComponentScan("org.example" )
@MapperScan(value = "org.example.mapper")//扫描mapper接口
public class MybatisConfig {
    @Bean//会话工厂
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource());//通过指定的方法获得对应的数据源
        //虽然我们已经用javabean完成了大量的工作，但是mybatis还是需要配置一个mybatis-config.xml文件
        factoryBean.setConfigLocation(new PathMatchingResourcePatternResolver().getResource(mybatisXml));//明确指定对应的mybatis配置文件
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/**/*Mapper.xml"));//导入对应的xml映射文件
        factoryBean.setTypeAliasesPackage("org.example.entity");
        //指定对应的实体类，并使用我们指定的别名
        return factoryBean.getObject();
    }
    @Bean
    public DataSourceTransactionManager transactionManager() {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource());
        return dataSourceTransactionManager;
    }
    @Value("classpath:property/mysql.properties")//这里我们将文件放入了resources/property目录下
    Properties resource;//通过@Value注解将指定的文件导入变量resource中
    @Value("classpath:mybatis/mybatis-config.xml")//同样是通过注解获取对应的文件
    String mybatisXml;
    @Value("org/**/*Mapper.xml")//指定mapper映射文件
    String mapperLocation;
    //下述的方法将进一步提取properties文件的内容
    @Bean
    public PropertiesFactoryBean proper(){
        PropertiesFactoryBean propertiesFactoryBean=new PropertiesFactoryBean();
        propertiesFactoryBean.setProperties(resource);
        return propertiesFactoryBean;
    }
    //上述的方法可在下方的注解用于提取属性名
    @Value("#{proper['jdbc.driver']}")
    private String driver;//注解直接获得对应的值
    @Value("#{proper['jdbc.username']}")
    private String username;//由于实际的值和属性名没有直接关系，可尽量避免硬编码
    @Value("#{proper['jdbc.password']}")
    private String password;//注意注解@Value是不能在方法内使用的
    @Value("#{proper['jdbc.url']}")
    private String url;
    //下方的方法生成对应的数据源
    public DataSource dataSource(){
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName(driver);
        druidDataSource.setUrl(url);
        druidDataSource.setUsername(username);
        druidDataSource.setPassword(password);
        /*除此之外，由于DruidDataSource继承了DruidAbstractDataSource，
          继承了许多关于连接池的配置方法，如配置连接池的大小，每个连接的存活时长等，
          读者可自行查看源码看看自己需要的方法*/
        return druidDataSource;
    }
}

```

 到这里，我们基本得到一个mybatis，当然还缺少一个mybatis-config.xml文件。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <!-- 使用jdbc的getGeneratedKeys获取数据库自增主键值 -->
        <setting name="useGeneratedKeys" value="true" />
        <!-- 使用列别名替换列名 默认:true -->
        <setting name="useColumnLabel" value="true" />
        <!-- 开启驼峰命名转换 -->
        <setting name="mapUnderscoreToCamelCase" value="true" />
    </settings>
    <typeAliases>
        <package name="org.example.entity"/><!--设置别名，指定基础的包-->
    </typeAliases>
</configuration>
```

如果读者学习过nybatis基础的用法，可能会发现这个xml文件过于简单，要的就是这个效果。

### 3. sql语句

经历了前面的操作，我们已经可以连接我们的数据库，为了实际对数据库进行操作，不仅需要知晓各个表的情况，还需要有简单的方法进行增删改查。

首先是表的情况，类似我们在springmvc中介绍的，为了方便数据的传递和操作，将原本的数据信息转化为对象，这里也是同样的操作，表的列名为对应的属性名。

这时候就会发现上面的xml文件明确写着开启驼峰命名，这一点在数据库创建的时候介绍了。我们需要做的就是创建对应的类。

```java
@Alias(value="USER")//上面的配置文件中指定了基础包，这里设置了该类将对应的别名
@Component
public class User {
    private Integer id;//需要注意的是，属性的类型尽量不要是基本类型
    //这类包装类当结果为空时，返回的是明确的null，否则可能是数字0,这样无法简单判断出对应列名是否为空
    private  Integer userId;
    private String userName;
	/* setter/getter方法 */
    //如果不想实现setter/getter，可以使用lombok的@Data隐性地实现这些方法
}
@Alias(value="STUDENT")
@Component
public class Student {
    private Integer id;
    private Integer studentId;
    private String studentSchool;
	/* setter/getter方法 */
}
```

对于普通用户进行使用时，自然是希望我们准备好简单的方法直接添加或读取指定的数据，但是这一切都需要我们首先完成若干条基本的SQL语句，有了这些基本的sql语句，我们才能更加方便地实现复杂的需求。

现在，我们就要创建对应的映射语句，先定义对应的接口，指明需要的方法，再由一个对应的xml文件完成对应的sql语句实现。

这里以user的表操作为例，

```java
@Mapper
public interface UserMapper {
    /*这里稍微插入一点介绍，接口中的方法是默认为public，正常而言，这些方法是不能在这里实现的。
    * 但是如果是静态方法则可以。
    * 另外，jdk8之后，可以添加default方法，可用于实现。*/
    //其实我们可以在这个接口类中用对应的@Insert或@Select注解完成映射语句，
    // 但是还是觉得直接在xml文件中实现比较清晰
    //用于插入指定的User对象
    Integer insertUser(User user);
    //根据用户名查询数据库中指定的记录，并且以对应的类返回结果
    User selectName(@Param(value = "name")String name);
    //如果方法参数的名字与xml映射语句中的参数名相同，则不需要额外用@Param指明对应xml中的参数名
    //挑选出大于指定id的用户
    List<User> FilterUser(@Param(value = "id")Integer id);
    //根据指定的用户名删除数据库中的记录，若成功删除，则返回删除的记录条数，否则返回为0
    //统计当前有多少人
    Integer countAll();
    Integer deleteUser(@Param(value = "name")String name);
    //按照给定的对象的user_id更新对应嗲user_name
    void updateName(User user);
}
```

接下来是对应的xml映射文件，【namespace指向对应的mapper接口】

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.example.mapper.UserMapper">
    <!--指明对象中的属性与实际的表中的列名的关系，实际有更复杂的功能，
        但我们这里只需要简单地指明二者之间的类型关系-->
    <!--方便我们下面使用resultType不引发无法判断的问题-->
    <resultMap id="UserMap" type="USER">
        <result column="id" jdbcType="INTEGER" property="id"/>
        <result column="user_id" jdbcType="INTEGER" property="userId"/>
        <result column="user_name" jdbcType="STRING" property="userName"/>
    </resultMap>
    <sql id="ut">user_table</sql><!--指定我们试图在sql语句中使用的语句的简化-->
    <!-- 这里我们提前简化了表名-->
    <sql id="user_column">id,user_id,user_name</sql>

    <!--这里的parameterType我们用先前指定的别名USER代替常规的org.example.entity.User-->
    <insert id="insertUser" parameterType="USER" useGeneratedKeys="true"
            keyProperty="id" keyColumn="id">
        <!--我们这里用<include refid="ut"/>指代了表名，
            可能觉得多此一举，但是当我们试图复制重用这些代码，那么，
            我们只需要修改前面的一小段内容即可-->
        <!--更复杂一点，sql标签内部也可以使用占位符，例如
            <sql id="suibianxie"> ${suibian}.id,${suibian}.username</sql>
            之后需要用到的时候，就如下
            <include refid="suibianxie"><property name="suibian"
                                        value="你需要的值"/></include>-->
        insert into <include refid="ut"/>
            (user_id,user_name) values(#{userId},#{userName})
    <!--我们这里没有配置对应的id值，因为它是自动增长的-->
    <!--我们用useGeneratedKeys="true"明确强调这一点，
        之后我们就需要指明哪些属性或列名受到影响-->
    <!--keyProperty="id" keyColumn="id"则分别指明了对象中的属性名和表中的列名，
        将自动获取自动增长的id值-->
    </insert>

    <!--这里我们就使用了resultType，
        sql语句中挑选出的列名都会按照resultMap的映射关系对应到对象中-->
    <select id="selectName" parameterType="string" resultType="USER">
        <!--如果我们传入的参数name是空的，那么就没有比较的价值了，
            因此需要排除这种情况带来的问题-->
        SELECT user_id, user_name
        FROM <include refid="ut"/>
            <where>
            <!--使用这种方式可以添加多条判断语句，常规下处于后面的语句需要加上and关键词,
                如果前面的语句失败而导致出现 where and 自定义语句，则出现语法错误，
                使用标签<where>则可以避免这一问题，即使我们加上了and或其它类似的关键词，
                只要前面的语句没有出现，mybatis会自动删除这些关键词-->
                <if test="name != null">
                    user_name=#{name}
                </if>
                <if test="user_name == null">
                    1=-1
                </if>
            </where>
        <!--上面介绍了标签《where>的作用，但实际，如果想用功能更广泛的标签，可以尝试<trim>,
            <where>的功能等价于
            <trim prefix="where" prefixOverrides="and | or ">
              ...
            </trim>-->
    </select>
    <!--因为项目中的表的列名可能非常多，
        尽量不要随便使用*获取所有列名，否则会影响数据库的执行效率-->
    <select id="FilterUser" parameterType="int" resultType="USER">
        <!--这里我们希望导入的参数不为空，且大于0-->
        select user_id,user_name from <include refid="ut"/>
                where 1=1
        <choose>
            <when test="userId != null and userId>0">
                and user_id>=#{user_id}
            </when>
            <otherwise>
                and 1=-1
                <!--当前面的判断false后，则出现空缺，该语句将被迫出现，
                并迫使当前结果失败，避免挑选出无关的结果-->
            </otherwise>
        </choose>
    </select>

    <select id="countAll" parameterType="int" resultType="Integer">
        select count(*) from <include refid="ut"/>
    </select>

    <delete id="deleteUser" parameterType="string">
        delete from <include refid="ut"></include>
                    where 1=-1
                    <if test="name != null">
                        or user_name=#{name}
                    </if>
    </delete>

    <update id="updateName" parameterType="USER">
        update <include refid="ut"></include>
            <!--这里也是可以使用<trim prefix="set" prefixOverrides=","></trim>代替-->
            set user_name=#{userName} where user_id=#{user_id}
            <set>
                <!--直接使用对象中的属性名-->
                <if test="userName != null">user_name=#{userName}</if>
                <if test="userId != null">user_id=#{userId}</if>
            </set>
    </update>
</mapper>
<!-- 如果我们需要判断user_id是否在一个指定的集合中，但是集合的内容可能又是带有一些乱七八糟的括号的，
    则需要有一个遍历器提取出对应的值，
    则引出标签<foreach>,
    <foreach item="随便写的内部变量名" index="随便写一个索引名"
             collection="被遍历对象的类型，如list或map"
             open="(" separator="," close=")">
             这里认为数据是存放在括号括起来的，逗号分隔的形式中
            #{item}
      </foreach>-->
```

~~另外，如果读者觉得xml文件前面的内容非常复杂且不易记忆，如果是使用IDEA的话，可以将基本的内容放入模板中，至于怎么搞定模板，可自行查询~~

> 现在我们得到了基本需要的数据库查询功能（当然了，不够自己再加）

------------

到这里，关于mybatis如何在spring环境下配置使用就基本完成。但很明显，读者发现这里讲了基本的操作，具体的使用怎么没有了。

问题就在于，我们现在有springBoot，原先的SSM框架需要额外的配置工作，到了springBoot就基本上可以省去很多xml文件。上述的工作大部分还是必须的，但最关键的是让读者先明白mybatis大致的情况。

我们接下来会使用springboot完成SSM框架的工作。请阅读springboot。































### 附录

#### mybais自带的别名列表

| 别名       | 映射的类型 |
| :--------- | :--------- |
| _byte      | byte       |
| _long      | long       |
| _short     | short      |
| _int       | int        |
| _integer   | int        |
| _double    | double     |
| _float     | float      |
| _boolean   | boolean    |
| string     | String     |
| byte       | Byte       |
| long       | Long       |
| short      | Short      |
| int        | Integer    |
| integer    | Integer    |
| double     | Double     |
| float      | Float      |
| boolean    | Boolean    |
| date       | Date       |
| decimal    | BigDecimal |
| bigdecimal | BigDecimal |
| object     | Object     |
| map        | Map        |
| hashmap    | HashMap    |
| list       | List       |
| arraylist  | ArrayList  |
| collection | Collection |
| iterator   | Iterator   |

