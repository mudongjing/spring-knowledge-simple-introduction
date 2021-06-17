## Mybatis基本使用【和springmvc】

> Mybatis其实就是一个方便我们操作数据库的工具，使用它，我们可以方便地存储和查询数据。
>
> 它是一种ORM框架（ORM即Obejct/Relation Mapping，对象/关系数据库映射）
>
> 官方也有[网站]([mybatis – MyBatis 3 | 入门](https://mybatis.org/mybatis-3/zh/getting-started.html))专门介绍了其使用方法

==本文使用的mybatis版本为3.5.7==

### 1. 预备操作

为了操作数据库，我们首先需要利用JDBC与数据库建立联系，再通过给定的方法传入sql语句，借着进行数据录入或结果读取。总之，这一系列的操作麻烦而且缺乏灵活性，更重要的是本身非常死板。

我们只需要将基本的信息，如数据库地址，用户名，密码等必要的信息传入，即可创建数据库连接。之后，我们就需要设计映射语句，使用方法调用对应的sql语句模板，完成对应的操作。

因此，看起来框架很复杂，但终究就是两个程序，创建连接和写sql语句。

但首先，本文假设读者对于这些数据库操作非常陌生，那么我们首先需要创建一个数据库进行操作。

==这里我们使用mysql数据库==

首先引入mysql的依赖，`mysql-connector-java`，顺便引入Mybatis的依赖，就是单纯的 `mybatis`。

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
  CREATE TABLE `USER` (#``叫重音符，这里其实不用加，但只是作为介绍搞出来
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
  CREATE TABLE `STUDENT` (
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



