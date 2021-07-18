[TOC]

#  逻辑结构

首先mysql的整体逻辑结构可以划分为：连接层、服务层、引擎层、存储层。

连接层自然就是我们如何登录进mysql。服务层则是我们介绍的重点，它提供了我们增删改查以及各种函数等主要功能。引擎层则是对应着不同存储引擎，如InnoDB等。存储层则对应着如何持久化或读取磁盘数据等。

我们这里着重说明一下服务层和引擎层。

```mermaid
flowchart TD;
    k(客户端) ;l(连接器);h(缓存区);cf(词法分析器);y(优化器);z(执行器);I(InnoDB);M(MyISAM);dd(其它引擎)
    subgraph connect [连接层]
        k-->l
    end
    
    l-->h
    l-->cf
    
    subgraph service [服务层]
        h;cf
        cf---->h
        cf-->y
        y-->z
        z-.->|结果|h
    end
    
    subgraph engine [引擎层]
       I ;M ; dd  
    end
    
    z-->engine
```

其中需要说明的是，缓存区在5.7之后被移除了。

但是，mysql版本8也是假如甲骨文后才搞出来的，应该说目前5.7或5.6版本的用户还是不少的。当然了，现在我们基本上都会用Redis作为缓存区，mysql自己的缓存似乎也确实是可有可无的。类似的，还有mysql自己搞的全文索引，就是利用倒排索引将特定内容的所在位置表示出来，即我们可能会使用的like，这个需要有，但是可以的话，我们就直接用ElasticSearch了。

- 我们通过各种方式进入mysql之后，在发送了查询命令后，首先会到词法分析器中，确定你的命令没问题，然后再到缓存区中查找是否有命中的结果。

  没有的话，再经过优化器，这是一个智能化的过程，如果你的查询命令包含了索引字段，那么自然会使用索引查找，而不是遍历，进一步的，如果事先我们建立的联合索引，优化器必然也需要考察一下给定的字段是否包含主键，以及按照怎样的字段查询顺序符合建立的索引树。

  最终，一切的策略规划好了，就有执行器去在表中进行查找或其它操作。

  引擎是管理表结构的，相当于一个插件，不同的表，我们可以随意设置它的引擎类型。它是真正负责建立记录的各种索引树等信息模块。

  最后，当从引擎中获取了结果后，执行器会顺便把结果放到缓存区中。

- 缓存区在写操作频繁时则有些难受。因为写操作也相当于一次新记录的查询，需要放到缓存区中，这样就会频繁地修改缓存区，使得原本单纯的增加记录，附带了一个修改缓存的操作。

- 由于缓存区存在一定的局限性，因此可在mysql的配置文件`my.cnf`中，设置

  ```bash
  query_cache_type=2 #意味着我们可以在命令语句中指定是否需要缓存
  #如
  #select SQL_CACHE 列名 from 表名; 此时就代表可以使用缓存
  #可以使用命令查看缓存的状态
  #show status like '%Qcache%'; 即对应的query_cache_type，5.7之后的bjbf基本上这条语句查不到什么
  #也可以查看自己的缓存是否开启
  #show variables like '%query_cache_type%';
  ```

- 设置连接等待秒数

  ```bash
  #设置全局服务器关闭非交互连接之前等待的秒数
  set global wait_timeout=xx;
  #设置全局服务器关闭交互连接之前等待的秒数
  set global interactive_timeout=xx;
  ```

# 索引

> 为什么mysql底层没有使用二叉树或红黑树。因为二者本质都是二叉树，一旦数据量巨大，无论其算法怎么优化，都会有巨大的层数。

读者都知道mysql的索引树建立使用的是B+树。至于B-树，读者也知道，就是一个多阶的平衡树，原本的二叉树之类的就是一个节点就包含一个信息，而B-树则可以包含指定最大数量的信息，并且每个信息又附带了它自己的详细数据。

关键就在于节点的信息附带数据，本来这样考虑是想着从磁盘读取信息的时候顺便拿上数据，如果匹配到的话就可以直接获取结果，避免再去走一遍缓慢的IO。

但是，mysql需要考虑的是，面对上千万的数据量，如何快速地找到目标，这要求树的高度要尽可能小。

mysql的B+树，就是把附带的数据去掉了，最后所有的结果都作为叶子节点堆积起来

> 【需要注意的是，因为B+树本来就是一种排序的树，最后的叶子节点对应的信息也是有序的，并且互相构建了一种类似双向链表，因此，如果搜索范围的话，得到一个叶子节点就可以轻松地获取其它结果】。
>
> 对于不同的数据结构，可以在这个[可视化网站](https://www.cs.usfca.edu/~galles/visualization/Algorithms.html)中自己尝试着添加数据看看得到的树到底是什么样。(需要注意的是，该网站的B+树叶子是单向链表，而mysql实际使用的是双向的)

叶子节点是附带数据的。这么做的话一个节点可以以较少的空间存储更多的信息，

> 比如一个节点只被分配了16个字节的空间，如果附带数据的话，数据可能就占据了3个字节，如果信息本身又占据1个字节，那么一个节点能保存的信息只有两个，那么其它百万的信息，按照这种规划需要多少节点，这些节点每次最多是能产生三个子节点，最终又会产生多高的树，每一层都意味着我们可能要通过一次IO读取对应节点的数据，可能树的高度有30层，单纯的内存操作，速度也许能接收，但是如果是IO，那就相当于在晚高峰开大卡车。

B+树建立的索引树，非叶子节点占据的空间非常小，如果索引的信息是一个biginteger (8B)，附带的指针是6B，总共就是14B。

> InnoDB中用页的概念，表示磁盘与内存传输数据的基本单位，默认是16KB,也就是对应着我们所说的节点的空间大小。【不过，操作系统的页是4KB的大小】

默认一个节点的大小是16KB，也就是可以保存约1170个信息，最后的叶子节点由于附带了数据，我们假设一个节点就保存10个记录，那么三层的索引树就可以保存$1170*1170*10=13689000$，可以发现此时就可以有一千多万的记录。

> 前面的索引占据很小的空间大小，可以常驻在内存中，于是内存可以在三层树上获取数据地址，通过一次IO就可以得到最终数据。

> 可能读者想说，如果这样，还分什么节点，直接扩大节点的空间大小，记录信息全部放进去不好吗。
>
> 其实也是可以的。但并不太普遍适用。
>
> 首先，系统从磁盘读取数据是按页为单位读取，一页是4kB，如果对应的几个页不在一起，可能还需要在磁盘上搜索到其它地方获取,甚至其它页读取失败，此时，单纯的读取数据就不那么单纯了。为了减少磁盘查找页的时间，就尽量不去依此提取太多的页。这也是设置节点的大小是4KB的整数倍的原因。
>
> 就算我们将索引常驻在内存中，避免了磁盘的各种因素。如果用户的主键是随机数，那么节点中的索引需要频繁地改动，若这个数据量巨大，千万条数据在一个节点里来回波动，这也是巨大的效率损失。
>
> 因此，如果索引主键是自增的整数，且没有删改，那么一个节点的操作也可以接受。自己小规模的项目可以试一试。

mysql的全文索引创建使用以下的语句，

```mysql
alter table  表名 add fulltext(列名);
```

## 主键的必要性

在表创建时用`primary key`指定哪个列为主键。

前面已经介绍了不同的引擎是负责进行表的索引管理。MyISAM会将索引和数据分开来保存到两个文件中，当从索引文件中获取数据的记录数，然后在从数据文件中利用记录数获取对应的数据。而InnoDB引擎则将二者包含在一起，这种称为聚集索引，而MyISAM称为非聚集索引。

>上面说明的文件也就是对应着存储层，引擎将读取对应的文件，从文件获取信息

那么索引具体是以什么信息作为排序的依据呢？可以是随便什么信息，数据就按数字的大小，字符串就按字典顺序。由于我们现在常用的就是InnoDB，而InnoDB特别强调要一个主键，并推荐是自增的整数。我们来说明一下原因。

索引的排序依据自然需要时表中唯一的，如果没有我们没有指定这样一列数据，mysql会自己费力地审查表中的各个列，直到找到符合条件的列，如果没有，就会自动生成一个列作为索引依据。

因此，最好自己指定主键。另外如果是使用字符串作为索引依据，那么字符串的大小比较要比数字比较麻烦很多，自然效率会损失很多。因此，建议使用整数，自增不就顺便唯一了嘛。

并且，因为主键的数字是递增的，那么对应的索引数，每次只需要将新的记录追加到树的尾部，再可能适当地调整数，而不是那种随机值导致插入到树的中间，影响后面的信息。

> 索引的建立也可以使用哈希表的方式实现，而且速度还更快。但是无法实现范围查找。
>
> ```mysql
> KEY USING HASH(列名) -- 在创建表时，可以指定用hash构建索引
> ```

## 联合索引

所谓联合就是依靠多个字段看作一个整体而视为一种主键，前提是这些字段的信息确实能够唯一地确定一条记录。

之前的索引建立时单纯按照主键的大小进行排序，而这里是依此按照字段的优先性进行排序，前面的字段如果相同，则依靠后面的字段大小进行排序。

这样的好处在于，通过一个索引树，一定程度地完成了多个索引的建立，方便了范围搜索，但前提是，搜索时必须附带上优先性最高的字段，否则mysql根本不知道从何开始，只能遍历。

联合索引的创建做法是，

```mysql
KEY `自定义索引名` (`列名1`, `列名2`, `等等`) # 这是在创建表的时候使用的
#如果表已经有了
alert table 表名 add INDEX `自定义索引名` (`列名1`, `列名2`, `等等`);
-- 或写
create index `自定义索引名` on 表名 (`列名1`, `列名2`, `等等`);
-- 其中，创建联合索引时，每个列名后面默认跟着一个asc，表示按顺序排序，当然我们可以明确写一个desc，表示该列按降序排
```

## 倒排索引

该索引的作用在于记录一个文档中不同单词所在的位置，当我们试图搜索某一语句，其中的单词将在文档中提取出包含对应单词的句子，读者此时就会发现这不就是搜索引擎的工作嘛，实际搜索引擎内部使用的ElasticSearch的底层机制也大致是这个操作。

倒排索引使用的是数组结构，在创建表的时候，可以指定对哪些列进行全文索引，这样我们就可以得到一个单词在哪些记录中的列中存在，我们这里姑且节这个字段称为文档（毕竟elasticsearch也是这么干的）

> 一种形式称为inverted file index：{单词，(文档1，文档2，...)}，
>
> 或者更具体的，
>
> 额外记录了内容中，单词所在的具体位置，称为full invertedindex：{单词，[(文档1: 位置1,位置2)，(文档2：位置1)]}
>
> 现在InnoDB采用的就是后一种形式。

我们在创建表的时候可以使用下述的方法指定全文索引的列，

```mysql
CREATE TABLE 表名 (
    -- 省略
    列名 TEXT,
    FULLTEXT 索引名 (列名) -- 可以指定列，没必要的列就别索引
) ENGINE=InnoDB;
-- 也可以在表创建之后创建全文索引
 CREATE FULLTEXT INDEX 索引名 ON 表名(列名);
 -- 也可以写为
 ALTER TABLE 表名 ADD index 索引名 FULLTEXT(列名)
 -- 需要注意的是，由于全文索引的创建比较耗资源，当表正在进行大量修改时，先不要创建全文索引
```

搜索的方法如下，

```mysql
select 列名 from 表名 match(列名) against(匹配模式);
```

- 具体实现

  上面主要介绍了倒排索引的大概形式和使用的方法。但是，具体到如何建立这样的索引，自然需要考虑到效率问题。

  首先，我们需要有一种表是存储单词的，InnoDB使用的是辅助表(Auxiliary Table)，并且有六张表，方便做并行处理。

  而我们真正进行全文索引的是另一张表，就是对应着full invertedindex的形式，一列是索引，指向单词在辅助表中的位置，另一列就是对应着在不同文档中的位置。

  这些是被持久化的内容，执行效率低。

  --------------

  为此需要一个全文检索索引缓存(FTS Index Cache)，很明显，这就是放在内存存放部分辅助表的内容，结构上是[红黑树](# 红黑树)，当进行各种操作时，主要是先在这个缓存中进行。

  当我们插入新的文档后，就增加了单词对应的位置，可能还添加了新的单词，如果直接在磁盘文件中操作，一个文档的加入，可能就涉及到辅助表的多次修改和插入操作。只有当事务提交后，这些缓存的信息才会持久化到磁盘中。

  > 如果关闭数据库，缓存中的内容也会自动持久化。缓存的空间满了，也会自动持久化。默认缓存大小是32M。

  辅助删除表(DELETED auxiliary table)

  > 另外，当执行删除操作时，当然也是现在缓存中操作，即使持久化，辅助表中的哪个内容也是不会删除的，而是由辅助删除表记录辅助表中被删除的id。
  >
  > 当然了，这些被删除的内容，终究时被抛弃的，存储的id数量太多，用户可以自己手动删除被遗弃的内容，命令有 `OPTIMIZE TABLE`或`innodb_optimize_fulltext_only`之类的。

  介绍到这里，我们发现这些操作中，标记文档的id非常重要，InnoDB也专门指定了这个id的格式，这个列必须命名为`FTS_DOC_ID`，其类型必须是`BIGINT UNSIGNED NOT NULL`，并且InnoDB存储引擎自动会在该列上加入一个名为`FTS_DOC_ID_INDEX`的唯一索引。

  比如，如下创建一个对应的表，

  ```mysql
  CREATE TABLE 表名(
  FTS_DOC_ID BIGINT UNSIGNED AUTO_INCREMENT NOT NULL,
  body TEXT,-- 对应的文档内容
  PRIMARY KEY(FTS_DOC_ID)
  );
  -- 经历了一通数据的添加后，在指明全文索引的列
  CREATE FULLTEXT INDEX 随便的索引名 ON 表名(body);
  ```

  此外，进行全文索引时，读者可能觉得，需要把其中的所有单词都搞一遍。但是，实际上有些单词就是普通的介词或感叹词，没有索引的价值，是没必要浪费时间索引它们。

  > 就需要，一个stopword表，指明哪些单词是不需要索引的，InnoDB有个默认的此类表`INNODB_FT_DEFAULT_STOPWORD`，给定了 36个单词。
  >
  > 我们也可以自定义一个，
  >
  > ```mysql
  > CREATE TABLE 表名(
  > value VARCHAR(最大字符数量)
  > )ENGINE=INNODB;
  > -- 然后指定该表是stopword表
  > SET GLOBAL innodb_ft_server_stopword_table="数据库名/表名";
  > ```

另外需要注意的是，InnoDB中查询单词时，有指定的长度范围，`innodb_ft_min_token_size`，
`innodb_ft_max_token_size`。

> 在InnoDB存储引擎中，参数innodb_ft_min_token_size的默认值为3，参数innodb_ft_max_token_size的默认值为84。

~~与其对mysql的全文索引进行深入了解，不不如去学习以下ElasticSearch的使用方法。不过ElasticSearch在我这个时间段早就开始停止正常的开源协议了。不过亚马逊搞得ES的分支，[OpenSearch](https://github.com/opensearch-project/OpenSearch)已经有1.0版本了，继承了开源协议~~

## 其它

==**对于聚簇索引，或称之为聚集索引，就是像InnoDB那样数据和索引放在一起的形式，它可以也可能产生了几个隐藏的索引列，可应对没有合适列做索引的情况，也可以在事务隔离的MVCC中的版本链中发挥作用。**==

msql中一行记录的最大字节数不能超过$2^{16}-1$ ，另外每一行不仅仅单纯记录对应的数据，还包括

> - 变长字段长度列表：针对varchar之类的可变量
>   - 我们在创建表时，会发现varchar(100)之类的格式，括号里的数字指明了这个字符串可以最多放多少个字符。其中也可以指定每个字符的编码格式，是原始的ASCII还是通用的utf-8，它们对应的一个字符的空间大小是不同的。
>   - 但是，mysql指定varchar不关心字符格式，只关心最多可以存放$2^{16}-1$的字节数。但这是骗人的，因为由于其它信息的存在，留给变量的空间不可能有这些字节数。
>
> - NULL值列表：针对一些可以设置为空的列，当对应值为空，这里可以标记，以节省空间
>
> - 记录头信息：这里包括
>   - `delete_mask`(记录该记录是否被删除)、
>   - `min_rec_mask`(若是B+树中非叶子层的最小记录的则需要被标记)、
>   - `n_owned`(该记录的记录数)、
>   - `heap_no`(该记录在记录堆中的位置信息)、
>   - `record_type`(该记录的类型，普通，或是B+树的非叶子节点，或是最小的记录，又或是最大的记录)、
>   - `next_record`(下一条记录的相对位置)

每条实际的记录还包括三个隐藏列，

> row_id：【行ID，唯一标识一条记录】不是必须的，占6个字节，
>
> transaction_id：事务的ID，必须的，占6个字节
>
> roll_pointer：回滚指针，必须的，占7个字节



#  日志

这里介绍的日志大多数是与InnoDB有关的。

## log

如果读者之前有了解过一点Redis的知识，应该会记得Redis中为了保证数据的安全，会定期的保存当前数据的快照，另外一种方式则是保存从开始到当前所有的操作，通过运行一遍操作就可以得到当前的结果。

mysql同样有这样的机制，而且有同样的两种方式：Binlog称为逻辑日志（是二进制日志），Redolog称为重做日志（物理日志）。此外还有undo log（回滚日志）。

> InnoDB为了保证事务的持久性，会首先将操作和操作的结果产生的日志先保存到磁盘中，其中InnoDB为了进一步保证事务的正确性（万一日志写入磁盘是出现故障，导致结果不完整，那么对应的信息也是没有价值的），就有了双写机制，即日志实际是写写到一个双写日志文件中，在从这个文件中转移内容，只要双写文件的内容是完整的就好，不完整也不会影响当前的日志内容。
>
> 双写机制可在[InnoDB的架构](# InnoDB架构)中看一下位置。
>
> 但是双写机制终究是比较影响效率的，可以关闭这一功能。

### bin log

这个日志是所有引擎通用的。主要记录哪些对数据库有改动的语句，像select之类的是不会记录的。因为我们把一开始的所有改动的语句执行一遍就可以得到当前的数据，所以，bin log文件是没有大小限制的，并且是不断在文件尾部追加新内容。

```mysql
show variables like 'log_bin';#查看自己是否开启了binlog
```

所有的变动操作都需要先写入这些日志文件，最后在挑选实际对数据库做实际的改动。

> 比如，我们适用delete删除数据，这一操作是先写入日志，但不一定数据的磁盘文件中就已经删除了，即使删除了，通过bin log文件，也可以返回到删除之前的状态。【当删除的内容很多时，则可能觉得速度慢，这就是一方面并不断调整数据库的内容结构，另一方面还需要写入日志】
>
> 但是，如果使用truncate命令来清空表，速度就非常快，一个原因就在于它是不写日志的，导致无法回溯。

由于bin log内容是二进制，我们需要使用`mysqlbinlog `命令来查看内容。

```bash
mysqlbinlog  --base64-output=decode-rows -v 对应的日志文件 
# base64等命令 是进一步解码文件 ，不写也大致能看出来主要内容
```

```mysql
show global variables like "%log_bin%";#查看日志的文件目录

#也可以在mysql中简单看一下日志文件记录的情况
show binary logs;#显示当前使用的日志文件，可能有多个
show master status\G;#当前被写入的日志文件
show binlog events in '日志文件名' \g #显示文件内部包含的事务情况
```

----------------------

我们这里在数据库my_test_base中创建了一个my_test的表，mysqlbinlog查看的部分日志内容表示为

```bash
# at 444                                                                            #210714 15:32:10 server id 1  end_log_pos 670 CRC32 0x33b4cc30  Query   thread_id=14    exec_time=0     error_code=0    Xid = 72              
use `my_test_base`/*!*/;                                                            
SET TIMESTAMP=1626247930/*!*/;                                                     /*!80013 SET @@session.sql_require_primary_key=0*//*!*/;                            
create table my_test(id int unsigned auto_increment,name varchar(255),primary key (id)) engine=InnoDB default charset=utf8
/*!*/;  
```

show binlog events查看的部分内容表示为

```bash
| Log_name      | Pos | Event_type | Server_id | End_log_pos | Info |             
| binlog.000012 | 444 | Query      | 1         |670          | use `my_test_base`; create table my_test(id int unsigned auto_increment,name varchar(255),primary key (id)) engine=InnoDB default charset=utf8 /* xid=72 */ |      
```

可以发现还是mysqlbinlog显示的内容稍微丰富一些。其中444，670对应的是在日志文件中的字节数位置。

```bash
#210714 15:32:10 //对应的是时间 2021-07-14 15：32：10
SET TIMESTAMP=1626247930/*!*/; //这个就是时间戳
```

现在我们就会发现表示指定的事务既可以使用对应的字节数，也可以用对应的时间，比如，

```mysql
mysqlbinlog 日志名 --start-position 起始字节数，如444 --stop-position 结束字节数，如670 | mysql -u 用户名 -D 数据库名 -p 密码;
# 或者
mysqlbinlog 日志名 --start-datetime 起始时间，如 "2021-07-14 15：32：10" --stop-datetime 结束时间 | mysql -u 用户名 -D 数据库名 -p 密码;
```

### redo log

这一日志负责的是记录磁盘中数据页的物理修改，不单纯是某几行的修改。当机器崩溃，我们需要用这个日志恢复到崩溃前的状态。

> 前面介绍了，读取磁盘数据最小单位是页，那么写入数据也是按页为单位写入的。
>
> redo就是记录这些页被修改的情况。

至于该日志的持久化，有WAL（Write Ahead Log）的做法，即修改修改数据库的磁盘数据前，先把内容写到日志中，再写数据库。

> 【因为数据库的操作更复杂，日志结构简单，写起来方便，总之先把新记录持久化再说】。

> redo log磁盘中的文件一般只有两个，而且有固定大小，对它的修改是在两个文件中循环写入，
>
> `innodb_log_files_in_group`可以设置实际的文件数量
>
> 如果redo log的文件被写满，则需要检查redo log中记录的日志对应的数据是否已经持久化到数据库的磁盘中，即使没有，也要强行让他持久化，那么此时这部分redo log的内容就已经无效了，因此可以删除写入新的日志内容。

而日志的持久化之前也不是那么单纯的内存传递过来，内存中redo log还有一块飞地，用于存储内容，等磁盘这里工作不忙了，再传递给系统缓冲区，最终写到磁盘中，称为落盘。也只有落盘后，mysql的事务提交才会显示成功。

> 到了系统缓冲区也不一定就当场写到磁盘中，为了保证安全性，InnoDB会额外调用 fsync操作，确保缓冲区的内容往磁盘写。
>
> 重做日志没有打开 O_DIRECT选项。【O_DIRECT选项是在Linux系统中的选项，使用该选项后，对文件进行直接IO操作，不经过文件系统缓存，直接写入磁盘)】
>
> `innodb_flush_log_at_trx_commit`参数，可控刷盘的策略
>
> - 0：表示事务提交时不进行写入redo log操作，这个操作仅在master thread 中完成，而在master thread中每1秒进行一次重做日志的fsync操作，因此实例 crash 最多丢失1秒钟内的事务。（master thread是负责将缓冲池中的数据异步刷新到磁盘，保证数据的一致性）
>
> - 1：（默认）表示事务提交时必须调用一次 `fsync` 操作，最安全的配置，保障持久性
> - 2：则在事务提交时只做 **write** 操作，只保证将redo log buffer写到系统的页面缓存中，不进行fsync操作，因此如果MySQL数据库宕机时 不会丢失事务，但操作系统宕机则可能丢失事务。

上述的工作主要是保证日志尽可能被持久化，可能我们commit了好几次，实际的数据库对应的磁盘的页并没有改变，而是要由checkpoint机制判断当前的时机是否合适，才能决定更新磁盘页。

>修改日志文件主要是在文件尾部追加，对于磁盘写内容而言是在连续的地址修改，更方便。
>
>而数据库对应的数据可能一次涉及多个页，这其中又可能涉及到数据库中表的索引的修改，总之，涉及的修改更复杂，远不是直接写入数据的事情。
>
>为了避免数据库更新带来的IO和其它因素的干扰，我们自然希望数据库的更新少一点。尤其是对一个页频繁的操作，我们IO数据库100次，和最后IO一次的效果是一样的。

```mermaid
flowchart TD;
qu1(读取指定数据);pan{数据在内存?};qu2(从磁盘读取指定数据__是按页为单位读进来);
duqu(CPU读取数据);xie(更新数据或写入新数据_还未commit);geng(数据更新到内存中);
buffer(数据更新写入redo_log_buffer中);bin(对应的binlog的内容写入到系统缓冲区中);
commit(commit_提交事务);xit(redo_log_buffer写入系统缓存);redofsync(调用fsync后_写入redolog文件中_返回commit成功);binfsync(将binlog系统缓冲区的内容写到磁盘中);check(checkpoint检查时机);shuju(更新数据实际所在的页);
qu1-->pan
pan -- 否 --> qu2
qu2-->duqu
pan -->|是| duqu
duqu-->xie
xie-->geng
geng-->buffer
buffer-->bin
bin-->commit
commit-->xit
xit--> binfsync 
binfsync -->redofsync
redofsync-->check
check-->shuju

style qu1 fill:#0460c4,color:#fff
style xie fill:#0460c4,color:#fff
style commit fill:#0460c4,color:#fff
style check fill:#0460c4,color:#fff
```

- mini-transaction

  前面提到了，操作的一个数据可能对应着多个页。因为redo log的记录就是以页为单位，这样原本的一个原子操作的sql语句，实际对应着多个redo log的原子记录，而我们需要额外考虑的就是，如何保证这些redo log记录能够原子化地加载的磁盘中的日志文件中。

  即，如果redo log对应的记录保存不完整，那么其它写入的记录也是无效的，且要求有序性。这就是mini-transaction需要做的工作。【如果记录失败，那所有当前录入的记录都不要写入】

  因为这些操作可能对应着不同的页，或是一个页中的不同位置，那我们就细化出多个小操作对应这些不同位置的操作，就对应着不同的mini-transaction。这样的化话，所有的redo log的原子操作基本上都会同时完成工作。

  每个mini-transaction都有自己的一个可变的内存缓冲区，当操作完成后，这些缓冲区再并发转移到真正日志的缓冲区，这里进一步地保证这些原子操作同时完成，且能够按照指定的循序进行加载，避免某些操作有较大的延迟而无法同时写入或顺序错乱。

  > 每个mini-transaction可以知道自己缓冲区的大小，一次确定下一个mini-transaction的起始位置。

  ```mermaid
  flowchart TD;
  redo1[原子操作1];redo2[原子操作2];
  rf1[缓冲区1];rf2[缓冲区2];
  subgraph mini [mini-transaction]
  
      subgraph mini1 [mini-transaction_1]
  		redo1-->buffer1
      end
      subgraph mini2 [mini-transaction_2]
  		redo2-->buffer2
      end
  end
  buffer1-->|拷贝|rf1
  buffer2-->|拷贝|rf2
  subgraph rebuf [Redo_log_buffer]
  	rf1 ;rf2;
  end
  subgraph refile [Redo_log_file]
  	文件1;文件2;
  end
  rebuf-->refile
  ```

- Block

### undo log

逻辑日志，记录被修改的数据的原始值，innodb特有的，用来回滚事务。保证了数据库的原子性。

执行undo的时候，仅仅是将数据从逻辑上恢复至事务之前的状态，而不是从物理页面上操作实现的，这一点是不同于redo log的。

大多数对数据的变更操作包括INSERT/DELETE/UPDATE，其中INSERT操作在事务提交前只对当前事务可见，因此产生的Undo日志可以在事务提交后直接删除（谁会对刚插入的数据有可见性需求呢！！），而对于UPDATE/DELETE则需要维护多版本信息，在InnoDB里，UPDATE和DELETE操作产生的Undo日志被归成一类，即update_undo。

一张表的某一行记录一旦被修改，undo log中就会增加一条内容，表示是哪个记录被哪个事务做了怎样的操作，同时指向该记录上次被修改的对应的内容。

undo log每次的内容主要有trx_id（修改该记录的事务id）、roll_pointer（指向上次该记录的修改内容）、row_id（一个隐藏列，可能作为主键）、 delete_mask（该记录是否被删除）、主键列。

### 慢查询日志



### 错误日志



## MVCC

MVCC（multiversion concurrency control）【多版本并发控制】。事务的隔离性就是有这一机制实现的。

我们知道，根据不同的隔离机制，不同事物之间互相产生的影响不同，但我们使用的mysql默认是可重复读，即对于当前事务而言，使用的数据库就像是额外的一个备份，对于其中的操作都限制在当前的事务内，不会对其它事务产生影响。

MVCC就是使用undo log对把不同事务的操作记录，提取出当前事务可以获取的数据。

首先，当我们开启一个事务时，mysql并没有为这个事务分配一个事务id，只有当该事务中执行了非查询类的操作，才会得到一个事务id。【事务id是不断增长的】

我们为了保证读取的数据具有可重复性，就需要明确我们当前事务处于的mysql的执行状态阶段，即对于我们创建事务的时刻，数据对应的哪些状态是我们可以读取的

> 如果我们创建事务时，另外有其它几个事务正在运行，那么对于我们而言，这些事务就是一直在运行的，即使它们后来提交了操作，数据库中的数据被它们修改，我们仍然不会认可它们修改的结果

> 于是，在我们创建事务时，需要指明当前有哪些活动的事务，意味着我们不会读取这些事务修改的数据。另外还需要记录当前最大的事务id，意味着之后如果碰到更大事务id，我们也不会关心它的结果。

我们当前的事务会生成一个read-view,其中一个数组记录了当前活动事务的id`[活动事务id1,活动事务id2,...]`,并记录当前最大的事务id。

活动事务id的最小值与最大事务id组成一个事务范围，若当前的事务试图读取一条记录，则进入undo log文件中，从最新的内容开始读取对应记录的修改内容，

【首先查看对应的deleted_mask视为为false】,如果对应的事务id小于事务范围，则读取，如果等于当前的事务id，也读取，除此之外，都属于不可读的记录，需要根据指针去查看上一个版本的记录，并继续判断，直到符合要求再读取数据。

# 锁

锁，总体上只有两种：共享锁和排他锁。共享也就是读取的操作，排他自然是写操作。

> 被设置为共享锁，则可以被很多进程、线程读取，但不允许写。被设置了排他锁，自然不允许其它任何用户使用。

> 但我们常用的单纯的select 查询语句，非常厉害，它不加锁，且任何锁也拦不住它

加锁的操作基本就是直接给表或给指定行加。

下面介绍的几种锁大多是概念上的，我们基本不会直接通过语句赋予那么具体的锁，但是mysql的底层确实存在对应的锁的算法，【很多这样的锁是mysql自己内部运行的】

```mysql
LOCK TABLES 表名1 write, 表名2 read; -- 表的读写锁,即可用于下述的意向锁
UNLOCK TABLES; -- 解除所有表的锁
-- 对行加锁
select [选择出一些行] lock in share mode; -- 加共享锁
select [选择出一些行] for share;  -- 8版本后的改动
select [选择出一些行] for update; -- 加排他锁
```

- 此外又引申出意向锁【作用于表】，如果表被标记为意向共享锁，那么其它试图占用整个表进行写入的进程则无法占用。如果被标记为意向排他锁，自然谁也无法占用该表。

  > 如果一个进程正在读取表中的某一行，那此时就可以把表标记为意向共享锁，或者正在写入某一行，也可以标记为意向排他锁。总之，都是为了快速地判断当前表是否可以被直接占用。
  >
  > 如果没有这样的意向表，那么占用一个表就需要一行行地判断是否有无法接受的占用，效率极为低下。

- 之后，又按照锁能控制的范围划分出表锁、页锁、行锁。【顾名思义】

~~下面将主要使用官方文档~~

## 表锁

- AUTO-INC锁

  AUTO-INC 锁是一种特殊的表级锁，由插入到具有 AUTO_INCREMENT 列的表中的事务获取。在最简单的情况下，如果一个事务正在向表中插入值，则任何其他事务都必须等待自己插入到该表中，以便第一个事务插入的行接收连续的主键值。 
  [innodb_autoinc_lock_mode](# innodb_autoinc_lock_mode) 变量控制用于自动增量锁定的算法。
  它允许您选择如何在可预测的自动增量值序列和插入操作的最大并发之间进行权衡。

- 表锁中还有一种称为元数据锁（meta data lock，MDL)，是mysql自动使用的，用于防止在查询的时候修改了表结构。

## 行锁

行锁又有一些变种，如记录锁、间隙锁、临键锁【InnoDB默认行锁算法】

行锁可以是一行也可以是多行，而记录锁只能是一行。【MyISAM不支持行锁】

> 其中当我们设置了主键自增后，该主键为了保证并发情况下字段是按1为单位的加，就需要有一个自增锁控制

- 需要注意的是，**两阶段锁协议。**
  - **在 InnoDB 事务中，行锁是在需要的时候才加上的，但并不是不需要了就立刻释放，而是要等到事务结束时才释放。**有可能出现死锁。

- 记录锁：

  ```mysql
  SELECT c1 FROM t WHERE c1 = 10 FOR UPDATE; -- 精准匹配
  ```

  记录锁是对索引记录的锁定。如果该表没有定义索引，那么InnoDB就会使用隐藏列作为索引标记该记录。

- 间隙锁

  ```mysql
   SELECT c1 FROM t WHERE c1 BETWEEN 10 and 20 FOR UPDATE;
  ```

  间隙锁是锁定索引记录之间的间隙，或锁定在第一个或最后一个索引记录之前的间隙。 

  间隙可能跨越单个索引值，多个索引值，甚至可能为空。

  另外，

  ```mysql
  SELECT * FROM child WHERE id = 100;-- 如果id是唯一索引，这无所谓
  -- 如果id不是唯一的，这个语句可能对应着多条记录，则可能需要间隙锁
  ```

  不同事务对同一间隙的间隙锁即使冲突，也是可以接受的。即不同事务之间的间隙锁是互不干扰的。

  - **插入意向锁**（Insert Intention Locks）

    插入意向锁是一种在行插入之前由 INSERT 操作设置的间隙锁。

    > 官方解释：
    >
    > 此锁表示插入意图，如果插入同一索引间隙的多个事务未插入间隙内的相同位置，则它们无需相互等待。
    > 假设存在值为 4 和 7 的索引记录。 分别尝试插入值 5 和 6 的单独事务，在获得插入行的排他锁之前，每个事务都使用插入意向锁锁定 4 和 7 之间的间隙，但不会相互阻塞，因为行不冲突。

    > 以下示例演示了在获取插入记录的排他锁之前采用插入意向锁的事务。该示例涉及两个客户端 A 和 B。
    >
    > 客户端 A 创建一个包含两条索引记录（90 和 102）的表，然后启动一个事务，对 ID 大于 100 的索引记录放置排他锁。 排他锁包括记录 102 之前的间隙锁：
    >
    > ```sql
    > mysql> CREATE TABLE child (id int(11) NOT NULL, PRIMARY KEY(id)) ENGINE=InnoDB;
    > mysql> INSERT INTO child (id) values (90),(102);
    > 
    > mysql> START TRANSACTION;
    > mysql> SELECT * FROM child WHERE id > 100 FOR UPDATE;
    > +-----+
    > | id  |
    > +-----+
    > | 102 |
    > +-----+
    > ```
    >
    > 客户端 B 开始一个事务以在间隙中插入一条记录。事务在等待获得排他锁时使用插入意向锁。
    >
    > ```sql
    > mysql> START TRANSACTION;
    > mysql> INSERT INTO child (id) VALUES (101);
    > ```

- 临键锁（Nex-key locks）

  > *InnoDB 以这样的方式执行行级锁定：当它搜索或扫描表索引时，它会在遇到的索引记录上设置共享锁或排它锁。*
  >
  > *因此，行级锁实际上是索引记录锁。 索引记录上的临键锁也会影响该 索引记录之前 的 间隙 。*

  也就是说，临键锁是索引记录锁定加上索引记录之前的间隙上的间隙锁定。

  示例：

  > 假设索引包含值10,11,13和20.此索引的可能的下一个键锁定覆盖以下间隔，其中圆括号表示排除间隔端点，方括号表示包含端点：
  >
  > ```
  > （负无穷大，10]
  > （10,11]
  > （11,13）
  > （13,20）
  > （20，正无穷大）
  > ```

  > 默认情况下， `InnoDB` 以 `可重复读`事务隔离级别运行。 在这种情况下， `InnoDB` 使用临键锁进行搜索和索引扫描，这会阻止[幻像行](# 幻像行)

## 全局锁

此外，mysql 还有一种全局读锁，可用于进行全局备份

```mysql
Flush tables with read lock;
```

## 空间索引的谓词锁

InnoDB 支持包含空间数据的列的空间索引（参见[优化空间分析](# 优化空间分析)）

为了处理涉及 SPATIAL 索引的操作的锁定，next-key 锁定不能很好地支持 REPEATABLE READ 或 SERIALIZABLE 事务隔离级别。多维数据中没有绝对排序的概念，所以不清楚哪个是“下一个”键。

为了支持具有 SPATIAL 索引的表的隔离级别，InnoDB 使用谓词锁。 SPATIAL 索引包含最小边界矩形 (MBR) 值，因此 InnoDB 通过在用于查询的 MBR 值上设置谓词锁来强制对索引进行一致读取。其他事务无法插入或修改与查询条件匹配的行。

# 集群

相信读者在学习是已经了解或听说了很多集群，redis，zookeeper，elasticsearch等，几乎我们用来管理数据的组件都可以实现集群化。其主要的形式也无非需要主节点存储和管理其它节点，以节从节点负责数据的备份，或者将大数据分配给多个节点共同存储。

mysql也不例外，它也可以构建集群。集群的形式划分，则在于是几个主节点，以及主节点之间的连接方式。

更多的细节在于，主从节点间的数据如何保持一致，以及主节点与从节点应该如何建立连接。这主要涉及到数据的安全和正确，与系统的拓展性。

## 复制

### 主从复制

这里是指主从复制，即包含主数据库和从数据库，我们需要保证的是，只能在主数据库中写数据，而从数据库只能复制主数据库中的内容。

> 也就是常说的实现读写分离，由于写数据会影响数据的一致性，因此需要在主数据库中操作，而读数据则可以在不同的从数据库中获得。

而主从模式又有不同的分支：

- 一主多从：由于主节点就一个，一旦该节点崩溃，对数据库就无法写入
- 双主双从：两个主节点可以互相通信，且各自有多个自己的从数据库。
  - 当一个主数据库被写入数据，对应的从节点首先同步数据，另一个主数据库才获得新数据，再同步到另一个从数据库
  - 每个主节点的从数据库可以有多个，不同主节点的从数据库是无法直接通信。从数据库只能同步自己主节点的数据
  - 该模式进一步增强了数据的写入的可靠性，但一个主节点崩溃后，它对应的从数据库也将无法之后的数据

### 复制流程

而具体如何复制主数据库的内容，则采用了bin log，我们直到bin log文件中记录的是数据库中被修改时经历的操作语句，只要我们从头执行一遍就可以得到当前的局面。

> 对于从数据库而言，需要做的就是没事就看一下主节点的bin log是否有更新，当有新数据了，则读取该日志文件，利用对应的命令提取对应的操作指令，施加到当前的数据库中。
>
> 至于如何发现日志有变动，只需要记录文件中的偏移量即可。只需要读取旧偏移量和新偏移量内的日志即可获得新的数据结果。
>
> 而这部分的日志内容需要先发送到从节点的中继日志文件中，再启用sql线程，调用这些命令，即同步成了主数据库的数据。【当然了，这时候从节点记录的那个日志文件的偏移量就需要修改了】
>
> ```mermaid
> flowchart TD;
> ndata(新数据)
> subgraph zhu [主数据库]
> 	zdata(主数据)
>  	zlog(bin_log文件)
> end
> ndata-->|2写数据|zdata
> zdata-->|3记录日志|zlog
> 
> subgraph fu [副数据库]
> 	fdata(副数据)
>  	flog(中继日志文件)
>  	sql(sql线程)
> end
> fdata -. 1监听-.->zlog
> zlog-.->|bin log发生更新|io((4调用IO))
> io-->|传递更新的日志内容|flog
> flog-->|5启动sql|sql
> sql-->|6操作新日志内容中的命令|fdata
> ```
>

## 集群搭建

主要的工作就是启动多个mysql实例，并配置对应的设置，指明哪些mysql实例是一个集群的，顺便再指明主节点。

mysql的配置文件在linux系统中是`my.cnf`，在windows中是`my.ini`，大致的格式可看一下[my.cof示例](#my.cnf示例)。但是，我们简单使用还没必要操作那么多的参数，在不同的机器上，只要把端口号、各类的日志文件或数据文件的路径指定好，基本就可以了，主要是，其中的`server_id`需要指定不同的数字。

> 使用的cnf文件，最好保持简洁，只保证需要的参数在里面，可以写注释，但不能写中文，即使是注释中。
>
> 读者可以大致设置以下的参数，
>
> ```bash
> [client]
> port=3306
> socket=   #通信载体，设置一个叫mysql.sock的文件路径
> default-character-set=utf8
> [mysqld]
> port=3306
> socket= 					#同样的，是那个文件的路径
> datadir= 					#数据文件所在的目录
> log-error= 					#错误日志error.log的路径
> pid-file= 					#mysql.pid文件的路径，记录其进程的id值
> character-set-server=utf8
> lower_case_table_names=1 	#不区分大小写
> autocommit=1				#自动提交
> log-bin=    				#指定bin log文件的名字，文件本身在datadir目录中，
> 										#并且只有主节点需要写这个参数
> server-id= 					#随便写，主要要求不同
> ```
>
> 然后启动mysql，并要求使用我们的这个配置
>
> ```bash
> mysqld_safe --defaults-file=my.cnf路径 &
> ```

### 主从关系

现在，我们虽然启动了这些mysql，但是它们之间还没有明确互相之间的那种合作关系。

> 如果，我们使用的服务器中的mysql之前是作为其它的从节点，最好重置一下
>
> ```mysql
> stop slave;
> reset slave;
> ```

在那个我们设定有bin log的服务器中【即我们希望是主节点的】，进入mysql，创建一个只能用于复制数据的账户

```mysql
grant replication slave on *.* to '自定义名字'@‘%’ identified by '设置一个密码'; 
-- 其中%可以直接指定该服务器的ip地址
reset master; -- 重置bin log
-- 因为此时bin log会记录我们创建了一个这样的用户，这条信息我们是不希望其它从数据库也执行的
-- 这条语句就将bin log的内容转变为之前的模样
```

然后，就是需要告诉其它的从节点，主节点是谁，并指定一个可以登录进去的账户，即我们刚才设置好的那个，

```mysql
change master to master_host='ip',master_user='对应的账户名',master_password='密码',master_port=端口号,master_log_file='具体的bin log文件名',master_log_pos=指定一个文件的偏移量;
-- 从节点将从指定的偏移量开始往后复制日志的内容
```

至于bin log文件的偏移量可以使用

```mysql
show master status;-- 当然需要在对应的主数据库执行
-- 其中的File即对应的 具体的bin log文件名 ，Position即对应着当前最新的偏移量
```

在执行了上述的各种命令后，从数据库就已经可以和主节点建立联系了，此时我们可以查看一下状态确认一下，

```mysql
show slave status \G;-- 自然需要在从数据库中执行
-- 可以显示出它的主节点的各种信息
-- 只有对应的·Slave_IO_Running· 和·Slave_SQL_Running·都是Yes,才说明一切的配置是没问题的
```

一切没问题后，我们就已经创建了一个一主多从的模式。

如果中间从数据库出现一些问题，可以直接重启试一试，

```mysql
stop salve;
start salve;
```

### 双主模式

该模式，就需要在上面设置的基础上做一点改进工作。

主要是设置对应的my.cnf文件，首先二者的`log-bin`的值要相同，

```bash
#在主数据库们中的my.cnf中添加以下参数
auto_increment_offset= #主键的起始点
auto_increment_increment= #指定一个数值，是当前数据库自增的步长
# 使用上述的参数，可以保证不同的主数据库的主键是各不相同的
# 因为，两个主数据库也会互相同步，一旦二者的某个主键id冲突，就会导致同步失败
# 进一步的，读者也可以搞成三主模式，N主模式
log-slave-updates #这句话表名主数据库从其它主数据库哪里同步过来的数据，也一样是写到bin log中
# 而我们知道，从数据库同步的内容是指就是bin log的内容
# 这样，从数据库就会间接同步了其它主数据库的内容
sync_binlog=1#指定提交一次事务，就同步一次bin log
# 1的情况，安全但低效，不怕数据丢失，可以写大点
# 0的情况，有文件系统决定了，或对应的缓存满了
```

照样启动一下数据库，同样在几个主数据库中设置一个只能复制的用户，同样，在几个从数据库中重置一下。

同样的，几个从数据库先找到自己各自的主数据库。

主数据库互相指定，也是采用的那种主从关系的命令，没有任何区别。

# 执行计划

所谓的执行计划并没有什么实际的作用，只是作为一个检查操作的辅助工具，基本用法就是在自己的语句前加上`expalin`就可以发挥该语句的相关执行参数，方便我们查看，例如select语句是否使用了索引，还是费力地使用了全文搜索。

下面组要摘自官网介绍，

- explain的输出结果包含的信息有，

| Column                          | JSON Name       | Meaning                     |
| ------------------------------- | --------------- | --------------------------- |
| [id](#id)                       | `select_id`     | `SELECT`标识符              |
| [select_type](#select_type)     | None            | `SELECT`类型                |
| [table](#table)                 | `table_name`    | 输出行 table                |
| [partitions](#partitions)       | `partitions`    | 匹配的分区                  |
| [type](#type)                   | `access_type`   | 联接类型                    |
| [possible_keys](#possible_keys) | `possible_keys` | 可能的索引选择              |
| [key](#key)                     | `key`           | 实际选择的索引              |
| [key_len](#key_len)             | `key_length`    | 所选键的长度                |
| [ref](#ref)                     | `ref`           | 与索引比较的列              |
| [rows](#rows)                   | `rows`          | 估计要检查的行              |
| [filtered](#filtered)           | `filtered`      | 按 table 条件过滤的行百分比 |
| [Extra](#extra)                 | None            | Additional information      |



# 附录

## 红黑树



## 事件类型

<table>
	<thead><tr><th>事件类型</th><th>说明</th></tr></thead>
	<tbody>
	<tr><td>UNKNOWN_EVENT</td><td>此事件从不会被触发，也不会被写入binlog中；发生在当读取binlog时，不能被识别其他任何事件，那被视为UNKNOWN_EVENT</td></tr>
	<tr><td>START_EVENT_V3</td><td>每个binlog文件开始的时候写入的事件，此事件被用在MySQL3.23 – 4.1，MYSQL5.0以后已经被 FORMAT_DESCRIPTION_EVENT 取代</td></tr>
	<tr><td>QUERY_EVENT</td><td>执行更新语句时会生成此事件，包括：create，insert，update，delete；	</td></tr>
	<tr><td>STOP_EVENT</td><td>当mysqld停止时生成此事件</td></tr>
	<tr><td>ROTATE_EVENT</td><td>当mysqld切换到新的binlog文件生成此事件，切换到新的binlog文件可以通过执行flush logs命令或者binlog文件大于 <code>max_binlog_size</code> 参数配置的大小；</td></tr>
	<tr><td>INTVAR_EVENT</td><td>当sql语句中使用了AUTO_INCREMENT的字段或者1436753函数；此事件没有被用在binlog_format为ROW模式的情况下</td></tr>
	<tr><td>LOAD_EVENT</td><td>执行LOAD DATA INFILE 语句时产生此事件，在MySQL 3.23版本中使用	</td></tr>
	<tr><td>SLAVE_EVENT</td><td>未使用</td></tr>
	<tr><td>CREATE_FILE_EVENT</td><td>执行LOAD DATA INFILE 语句时产生此事件，在MySQL4.0和4.1版本中使用</td></tr>
	<tr><td>APPEND_BLOCK_EVENT</td><td>执行LOAD DATA INFILE 语句时产生此事件，在MySQL4.0版本中使用</td></tr>
	<tr><td>EXEC_LOAD_EVENT</td><td>执行LOAD DATA INFILE 语句时产生此事件，在MySQL4.0和4.1版本中使用</td></tr>
	<tr><td>DELETE_FILE_EVENT</td><td>执行LOAD DATA INFILE 语句时产生此事件，在MySQL4.0版本中使用</td></tr>
	<tr><td>NEW_LOAD_EVENT</td><td>执行LOAD DATA INFILE 语句时产生此事件，在MySQL4.0和4.1版本中使用</td></tr>
	<tr><td>RAND_EVENT</td><td>执行包含RAND()函数的语句产生此事件，此事件没有被用在binlog_format为ROW模式的情况下</td></tr>
	<tr><td>USER_VAR_EVENT</td><td>执行包含了用户变量的语句产生此事件，此事件没有被用在binlog_format为ROW模式的情况下</td></tr>
	<tr><td>FORMAT_DESCRIPTION_EVENT</td><td>描述事件，被写在每个binlog文件的开始位置，用在MySQL5.0以后的版本中，代替了START_EVENT_V3</td></tr>
	<tr><td>XID_EVENT</td><td>支持XA的存储引擎才有，本地测试的数据库存储引擎是innodb，所有上面出现了XID_EVENT；innodb事务提交产生了QUERY_EVENT的BEGIN声明，QUERY_EVENT以及COMMIT声明，如果是myIsam存储引擎也会有BEGIN和COMMIT声明，只是COMMIT类型不是XID_EVENT</td></tr>
	<tr><td>BEGIN_LOAD_QUERY_EVENT</td><td>执行LOAD DATA INFILE 语句时产生此事件，在MySQL5.0版本中使用</td></tr>
	<tr>
<td>EXECUTE_LOAD_QUERY_EVENT</td>
<td>执行LOAD DATA INFILE 语句时产生此事件，在MySQL5.0版本中使用</td>
</tr>
<tr>
<td>TABLE_MAP_EVENT</td>
<td>用在binlog_format为ROW模式下，将表的定义映射到一个数字，在行操作事件之前记录（包括：WRITE_ROWS_EVENT，UPDATE_ROWS_EVENT，DELETE_ROWS_EVENT）</td>
</tr>
<tr>
<td>PRE_GA_WRITE_ROWS_EVENT</td>
<td>已过期，被 WRITE_ROWS_EVENT 代替</td>
</tr>
<tr>
<td>PRE_GA_UPDATE_ROWS_EVENT</td>
<td>已过期，被 UPDATE_ROWS_EVENT 代替</td>
</tr>
<tr>
<td>PRE_GA_DELETE_ROWS_EVENT</td>
<td>已过期，被 DELETE_ROWS_EVENT 代替</td>
</tr>
<tr>
<td>WRITE_ROWS_EVENT</td>
<td>用在binlog_format为ROW模式下，对应 insert 操作</td>
</tr>
<tr>
<td>UPDATE_ROWS_EVENT</td>
<td>用在binlog_format为ROW模式下，对应 update 操作</td>
</tr>
<tr>
<td>DELETE_ROWS_EVENT</td>
<td>用在binlog_format为ROW模式下，对应 delete 操作</td>
</tr>
<tr>
<td>INCIDENT_EVENT</td>
<td>主服务器发生了不正常的事件，通知从服务器并告知可能会导致数据处于不一致的状态</td>
</tr>
<tr>
<td>HEARTBEAT_LOG_EVENT</td>
<td>主服务器告诉从服务器，主服务器还活着，不写入到日志文件中</td>
</tr>
</tbody>
</table>

----摘自[MySQL Binlog 介绍]([MySQL Binlog 介绍 (juejin.cn)](https://juejin.cn/post/6844903794073960455))

## 幻像行

当同一查询在不同时间产生不同的行集时， 所谓的 幻像 问题发生在事务中。 例如，如果a [`SELECT`](http://www.deituicms.com/mysql8cn/cn/sql-syntax.html#select) 执行两次，但第二次返回第二次没有返回的行，则该行是 “ 幻像 ” 行。

假设 表 的 `id` 列 上有一个索引， `child` 并且您要读取并锁定标识符值大于100的表中的所有行，以便稍后更新所选行中的某些列：

```
SELECT * FROM child WHERE id> 100 FOR UPDATE;
```

查询从 `id` 大于100 的第一个记录开始扫描索引 。让表包含 `id` 值为90和102的行。如果在扫描范围内的索引记录上设置的锁不会锁定在间隙中进行的插入（在这种情况下，90和102之间的差距），另一个会话可以 `id` 在101中以101的形式 在表中插入一个新行 。如果要在同 [`SELECT`](http://www.deituicms.com/mysql8cn/cn/sql-syntax.html#select) 一个事务中 执行相同 的操作，则会看到一个 `id` 101 的新行 （一个 “ 幽灵 ” ）在查询返回的结果集中。 如果我们将一组行视为数据项，则新的幻像子将违反事务应该能够运行的事务的隔离原则，以便它在事务期间读取的数据不会更改。

为了防止幻像， `InnoDB` 使用一种称为 临键锁 的算法 ， 该算法 将索引行锁定与间隙锁定相结合。 `InnoDB` 以这样的方式执行行级锁定：当它搜索或扫描表索引时，它会在遇到的索引记录上设置共享锁或排它锁。 因此，行级锁实际上是索引记录锁。 此外，索引记录上的临键锁也会影响该 索引记录之前 的 “ 间隙 ” 。 也就是说，临键锁是索引记录锁定加上索引记录之前的间隙上的间隙锁定。 如果一个会话在记录中具有共享或独占锁定 `R` 在索引中，另一个会话不能 `R` 在索引顺序 之前的间隙中插入新的索引记录 。

当 `InnoDB` 扫描索引，它也可以锁定在指数的最后一个记录之后的间隙。 恰好在前面的示例中发生：为了防止任何插入到表中 `id` 大于100的锁，设置的锁 `InnoDB` 包括在 `id` 值102之后 的间隙上的锁 。

您可以使用临键锁在应用程序中实现唯一性检查：如果您在共享模式下读取数据并且没有看到要插入的行的副本，那么您可以安全地插入行并知道在读取期间在行的后继上设置的临键锁可防止任何人同时为您的行插入副本。 因此，临键锁使您能够 “ 锁定 ” 表中某些内容的不存在。

## innodb_autoinc_lock_mode

| Command-Line Format                 | `--innodb-autoinc-lock-mode=#` |
| :---------------------------------- | ------------------------------ |
| System Variable                     | `innodb_autoinc_lock_mode`     |
| Scope                               | Global                         |
| Dynamic                             | No                             |
| [`SET_VAR`](# SET_VAR) Hint Applies | No                             |
| Type                                | Integer                        |
| Default Value                       | `2`                            |
| Valid Values                        | `0`,`1`,`2`,                   |

用于生成自动增量值的锁定模式。
允许值为 0、1 或 2，分别表示传统、连续或交错。
从 MySQL 8.0 开始，默认设置为 2（交错），在此之前为 1（连续）。
将交错锁模式更改为默认设置反映了从基于语句的复制更改为作为默认复制类型的基于行的复制，这发生在 MySQL 5.7 中。
基于语句的复制需要连续的自增锁模式，以保证给定的SQL语句序列以可预测和可重复的顺序分配自增值，而基于行的复制对SQL语句的执行顺序不敏感

## SET_VAR

SET_VAR 进行临时变量更改，如以下语句所示：

```sql
mysql> SELECT @@unique_checks;
+-----------------+
| @@unique_checks |
+-----------------+
|               1 |
+-----------------+
mysql> SELECT /*+ SET_VAR(unique_checks=OFF) */ @@unique_checks;
+-----------------+
| @@unique_checks |
+-----------------+
|               0 |
+-----------------+
mysql> SELECT @@unique_checks;
+-----------------+
| @@unique_checks |
+-----------------+
|               1 |
+-----------------+
```

使用 SET_VAR，无需保存和恢复变量值。这使您能够用单个语句替换多个语句。考虑以下语句序列：

```sql
SET @saved_val = @@SESSION.var_name;
SET @@SESSION.var_name = value;
SELECT ...
SET @@SESSION.var_name = @saved_val;
```

该序列可以用以下单个语句替换：

```sql
SELECT /*+ SET_VAR(var_name = value) ...
```

独立的 SET 语句允许使用以下任何语法来命名会话变量：

```sql
SET SESSION var_name = value;
SET @@SESSION.var_name = value;
SET @@.var_name = value;
```

由于 SET_VAR 提示仅适用于会话变量，因此会话范围是隐式的，并且不需要也不允许 SESSION、@@SESSION. 和 @@。包含显式会话指示器语法会导致 SET_VAR 提示被忽略并发出警告。并非所有会话变量都允许与 SET_VAR 一起使用。单独的系统变量描述表明每个变量是否是可提示的；您还可以通过尝试将系统变量与 SET_VAR 一起使用来在运行时检查系统变量。如果变量不可提示，则会出现警告：

```sql
mysql> SELECT /*+ SET_VAR(collation_server = 'utf8') */ 1;
+---+
| 1 |
+---+
| 1 |
+---+
1 row in set, 1 warning (0.00 sec)

mysql> SHOW WARNINGS\G
*************************** 1. row ***************************
  Level: Warning
   Code: 4537
Message: Variable 'collation_server' cannot be set using SET_VAR hint.
```

SET_VAR 语法只允许设置单个变量，但可以给出多个提示来设置多个变量：

```sql
SELECT /*+ SET_VAR(optimizer_switch = 'mrr_cost_based=off')
           SET_VAR(max_heap_table_size = 1G) */ 1;
```

如果多个具有相同变量名称的提示出现在同一语句中，则应用第一个提示，并忽略其他提示并显示警告：

```sql
SELECT /*+ SET_VAR(max_heap_table_size = 1G)
           SET_VAR(max_heap_table_size = 3G) */ 1;
```

在这种情况下，第二个提示将被忽略并警告它是冲突的。
如果没有系统变量具有指定的名称或变量值不正确，则会忽略 SET_VAR 提示并发出警告：

```sql
SELECT /*+ SET_VAR(max_size = 1G) */ 1;
SELECT /*+ SET_VAR(optimizer_switch = 'mrr_cost_based=yes') */ 1;
```

对于第一条语句，没有 max_size 变量。
对于第二个语句，mrr_cost_based 取值 on 或 off，因此尝试将其设置为 yes 是不正确的。
在每种情况下，提示都会被忽略并发出警告。 
SET_VAR 提示仅在语句级别允许。如果在子查询中使用，提示将被忽略并显示警告。
副本忽略复制语句中的 SET_VAR 提示，以避免潜在的安全问题。

## 优化空间分析

对于 MyISAM 和 InnoDB 表，可以使用 SPATIAL 索引优化包含空间数据的列中的搜索操作。
最典型的操作是： 

- 搜索包含给定点的所有对象的点查询 
- 搜索与给定区域重叠的所有对象的区域查询

MySQL 对空间列上的 SPATIAL 索引使用带有二次分裂的 R-Trees。空间索引是使用几何图形的最小边界矩形 (MBR) 构建的。对于大多数几何图形，MBR 是围绕几何图形的最小矩形。对于水平或垂直线串，MBR 是退化为线串的矩形。对于一个点，MBR 是一个退化为该点的矩形。

也可以在空间列上创建普通索引。在非 SPATIAL 索引中，您必须为除 POINT 列之外的任何空间列声明前缀。

MyISAM 和 InnoDB 支持 SPATIAL 和非 SPATIAL 索引。其他存储引擎支持非空间索引。

## InnoDB 架构

![5.7版本](https://dev.mysql.com/doc/refman/5.7/en/images/innodb-architecture.png "Mysql 5.7 版本")

<center style="color:#C0C0C0;text-decoration:underline">
   5.7版本
</center>

![8.0](https://dev.mysql.com/doc/refman/8.0/en/images/innodb-architecture.png "Mysql 8.0 版本")

<center style="color:#C0C0C0;text-decoration:underline">
   8.0版本
</center>
## 执行计划附录

### id

SELECT标识符。这是查询中SELECT的序号。如果该行引用其他行的并集结果，则值可以为`NULL`。在这种情况下，`table`列显示类似于`<unionM,N>`的值，以指示该行引用具有`id`值* `M` *和* `N` *的行的并集。

### select_type

SELECT的类型，可以是下 table 中显示的任何类型。 JSON 格式的`EXPLAIN`会将`SELECT`类型公开为`query_block`的属性，除非它是`SIMPLE`或`PRIMARY`。

| `select_type`值        | JSON Name                    | Meaning                                                      |
| ---------------------- | ---------------------------- | ------------------------------------------------------------ |
| `SIMPLE`               | None                         | 简单的SELECT(不使用UNION或子查询)                            |
| `PRIMARY`              | None                         | Outermost SELECT                                             |
| UNION                  | None                         | UNION中的第二个或更高版本的SELECT语句                        |
| `DEPENDENT UNION`      | `dependent` ( `true` )       | UNION中的第二个或更高版本的SELECT语句，具体取决于外部查询    |
| `UNION RESULT`         | `union_result`               | UNION的结果。                                                |
| SUBQUERY               | None                         | 子查询中的前SELECT个                                         |
| `DEPENDENT SUBQUERY`   | `dependent` ( `true` )       | 子查询中的第一个SELECT                                       |
| `DERIVED`              | None                         | Derived table                                                |
| `MATERIALIZED`         | `materialized_from_subquery` | Materialized subquery                                        |
| `UNCACHEABLE SUBQUERY` | `cacheable` ( `false` )      | 子查询，其结果无法缓存，必须针对外部查询的每一行重新进行评估 |
| `UNCACHEABLE UNION`    | `cacheable` ( `false` )      | 属于不可缓存子查询的UNION中的第二个或更高版本的选择(请参见`UNCACHEABLE SUBQUERY`) |

`DEPENDENT SUBQUERY`评估不同于`UNCACHEABLE SUBQUERY`评估。对于`DEPENDENT SUBQUERY`，子查询仅针对其外部上下文中变量的每组不同值重新评估一次。对于`UNCACHEABLE SUBQUERY`，将为外部上下文的每一行重新评估子查询。

子查询的可缓存性与查询结果在查询缓存中的缓存不同。子查询缓存在查询执行期间发生，而查询缓存仅在查询执行完成后才用于存储结果。

当您将`EXPLAIN`指定为`FORMAT=JSON`时，输出将没有与`select_type`直接等效的单个属性； `query_block`属性对应于给定的`SELECT`。可以使用与刚刚显示的大多数`SELECT`子查询类型等效的属性(示例是`MATERIALIZED`的`materialized_from_subquery`)，并在适当时显示。 `SIMPLE`或`PRIMARY`没有 JSON 等效项。

非SELECT语句的`select_type`值显示受影响 table 的语句类型。例如，对于DELETE条语句，`select_type`是`DELETE`。

### table

输出行所引用的 table 的名称。这也可以是以下值之一：

- `<unionM,N>`：行是指具有和`N`的`id`值的行的并集。
  - `<derivedN>`：该行引用具有`id`值* `N` *的行的派生 table 结果。派生 table 可能来自例如`FROM`子句中的子查询。
  - `<subqueryN>`：该行引用该行的物化子查询的结果，该子查询的`id`值为* `N` *。
- `partitions`(JSON 名称：`partitions`)

查询将从中匹配记录的分区。对于非分区 table，该值为`NULL`。

### partions 

查询将从中匹配记录的分区。对于非分区 table，该值为`NULL`。

### type

从最佳类型到最差类型：

| 类型            | 含义                                                         |
| --------------- | ------------------------------------------------------------ |
| system          | 该 table 只有一行(=系统 table)。这是const连接类型的特例。    |
| const           | 使用主键或者唯一索引，且匹配的结果只有一条记录。因为只有一行，所以优化器的其余部分可以将这一行中列的值视为常量。 const table 非常快，因为它们只能读取一次。 |
| eq_ref          | 在`join`查询中使用`PRIMARY KEY`or`UNIQUE NOT NULL`索引关联。                                                  对于先前 table 中的每行组合，从此 table 中读取一行。除了system和const类型，这是最好的联接类型。当连接使用索引的所有部分且索引为`PRIMARY KEY`或`UNIQUE NOT NULL`索引时使用。 |
| ref             | 使用非唯一索引查找数据。                                                                                                                     对于先前 table 中的行的每种组合，将从该 table 中读取具有匹配索引值的所有行。如果联接仅使用键的最左前缀，或者键不是`PRIMARY KEY`或`UNIQUE`索引(换句话说，如果联接无法基于键值选择单个行)，则使用ref。如果使用的键仅匹配几行，则这是一种很好的联接类型。 |
| fulltext        | 使用全文索引                                                 |
| ref_or_null     | 对`Null`进行索引的优化的 ref。                                                                                                      这种连接类型类似于ref，但是 MySQL 会额外搜索包含`NULL`值的行。此联接类型优化最常用于解析子查询。 |
| index_merge     | 此联接类型指示使用索引合并优化。在这种情况下，输出行中的`key`列包含使用的索引列 table，而`key_len`包含使用的索引的最长键部分的列 table。 |
| unique_subquery | 在子查询中使用 eq_ref。                                      |
| index_subquery  | 在子查询中使用 ref。此连接类型类似于unique_subquery。它代替了`IN`个子查询， |
| range           | 索引范围查找。                                                                                                                       使用索引选择行，仅检索给定范围内的行。输出行中的`key`列指示使用哪个索引。 `key_len`包含已使用的最长键部分。此类型的`ref`列是`NULL`。                                      使用=，<>，>，>=，<，<=，IS NULL，<=>，BETWEEN，LIKE或IN()运算符将键列与常量进行比较时，可以使用range。 |
| index           | 遍历索引。                                                                                                                        index联接类型与ALL相同，只是扫描了索引树。                                                                这发生两种方式：                                                                                                                  如果索引是查询的覆盖索引，并且可用于满足 table 中所需的所有数据，则仅扫描索引树。在这种情况下，`Extra`列 table 示`Using index`。仅索引扫描通常比ALL快，因为索引的大小通常小于 table 数据。  使用对索引的读取执行全 table 扫描，以按索引 Sequences 查找数据行。 `Uses index`没有出现在`Extra`列中。 |
| ALL             | 扫描全表数据  。                                                                                                                     对来自先前 table 的行的每个组合进行全 table 扫描。如果该 table 是第一个未标记const的 table，则通常不好，并且在所有其他情况下通常“非常”糟糕。通常，您可以通过添加索引来避免ALL，这些索引允许基于早期 table 中的常量值或列值从 table 中检索行。 |

### possible keys

possible_keys 列指示 MySQL 可以选择从中查找该表中行的索引。请注意，此列完全独立于 EXPLAIN 的输出中显示的表的顺序。这意味着可能_keys 中的某些键实际上可能无法与生成的表顺序一起使用。

如果此列为 NULL（或在 JSON 格式的输出中未定义），则没有相关索引。在这种情况下，您可以通过检查 WHERE 子句来检查它是否引用了一些适合索引的列，从而提高查询的性能。如果是，请创建适当的索引并再次使用 EXPLAIN 检查查询。
要查看表具有哪些索引，请使用 SHOW INDEX FROM tbl_name。

### key

key 列表示 MySQL 实际决定使用的键（索引）。如果 MySQL 决定使用 possible_keys 索引之一来查找行，则该索引被列为键值。 

key 可能会命名一个在 possible_keys 值中不存在的索引。如果所有 possible_keys 索引都不适合查找行，但查询选择的所有列都是某个其他索引的列，则会发生这种情况。也就是说，命名索引覆盖了选定的列，因此虽然它不用于确定要检索哪些行，但索引扫描比数据行扫描更有效。

对于 InnoDB，即使查询也选择了主键，二级索引也可能覆盖选定的列，因为 InnoDB 将主键值与每个二级索引一起存储。如果 key 为 NULL，则 MySQL 找不到可用于更有效地执行查询的索引。

要强制 MySQL 使用或忽略在 possible_keys 列中列出的索引，请在查询中使用 FORCE INDEX、USE INDEX 或 IGNORE INDEX。

对于 MyISAM 表，运行 ANALYZE TABLE 有助于优化器选择更好的索引。对于 MyISAM 表， myisamchk --analyze 执行相同的操作。


### key_len

key_len 列表示 MySQL 决定使用的键的长度。 key_len 的值使您能够确定 MySQL 实际使用多部分键的多少部分。如果键列显示为 NULL，则 key_len 列也显示为 NULL。

由于键存储格式的原因，可以为 NULL 的列的键长度比 NOT NULL 列的键长度大一。

### ref

ref 列显示哪些列或常量与键列中命名的索引进行比较以从表中选择行。

如果值为 func，则使用的值是某个函数的结果。要查看哪个函数，请在 EXPLAIN 后面使用 SHOW WARNINGS 来查看扩展的 EXPLAIN 输出。该函数实际上可能是一个运算符，例如算术运算符。

### rows

rows列表示 MySQL 认为它必须检查以执行查询的行数。

对于 InnoDB 表，这个数字是一个估计值，可能并不总是准确的。

### filtered

过滤列指示按表条件过滤的表行的估计百分比。最大值为 100，这意味着没有发生行过滤。从 100 开始减小的值表示过滤量增加。行显示检查的估计行数，行 × 过滤显示与下表连接的行数。

例如，如果行数为 1000，过滤为 50.00 (50%)，则与下表连接的行数为 1000 × 50% = 500。

### Extra

此列包含有关 MySQL 如何解析查询的附加信息。

没有与 Extra 列对应的单个 JSON 属性；但是，此列中可能出现的值作为 JSON 属性或消息属性的文本公开。

## my.cnf示例

不同的版本可能有那么一点点的不同，具体的可以查询对应的文档，但大致如此，windows版本下的my.ini内容也是相同的。

```bash
#
# FromDual configuration file template for MySQL, Galera Cluster, MariaDB and Percona Server
# Location: %MYCNF%
# This template is intended to work with MySQL 5.7 and newer and MariaDB 10.3 and newer
# Get most recent updated from here:
# https://www.fromdual.com/mysql-configuration-file-sample
#

[client]

port                           = %PORT%                              # default 3306
socket                         = %SOCKET%                            # Use mysqld.sock on Ubuntu, conflicts with AppArmor otherwise


[mysql]

no_auto_rehash
max_allowed_packet             = 16M
prompt                         = '\u@\h [\d]> '                      # 'user@host [schema]> '
default_character_set          = utf8                                # Possibly this setting is correct for most recent Linux systems


[mysqldump]

max_allowed_packet             = 16M


[mysqld_safe]                                                        # Becomes sooner or later obsolete with systemd

open_files_limit               = 8192                                # You possibly have to adapt your O/S settings as well
user                           = mysql
log-error                      = %INSTANCEDIR%/log/%UNAME%_%INSTANCE%_error.log   # Adjust AppArmor configuration: /etc/apparmor.d/local/usr.sbin.mysqld


[mysqld]

# Connection and Thread variables

port                           = %PORT%                                # default 3306
socket                         = %SOCKET%                              # Use mysqld.sock on Ubuntu, conflicts with AppArmor otherwise
basedir                        = %BASEDIR%
datadir                        = %DATADIR%
# tmpdir                         = '%INSTANCEDIR%/tmp'

max_allowed_packet             = 16M
default_storage_engine         = InnoDB
# explicit_defaults_for_timestamp = 1                                  # MySQL 5.6 ff. default in MySQL 8.0, test carefully! This can have an impact on application.
# disable_partition_engine_check  = true                               # Since MySQL 5.7.17 to 5.7.20. To get rid of nasty message in error log

# character_set_server           = utf8mb4                             # For modern applications, default in MySQL 8.0
# collation_server               = utf8mb4_general_ci


max_connections                = 151                                 # Values < 1000 are typically good
max_user_connections           = 145                                 # Limit one specific user/application
thread_cache_size              = 151                                 # Up to max_connections makes sense


# Query Cache (does not exist in MySQL 8.0 any more!)

# query_cache_type               = 1                                   # Set to 0 to avoid global QC Mutex, removed in MySQL 8.0
# query_cache_size               = 32M                                 # Avoid too big (> 128M) QC because of QC clean-up lock!, removed in MySQL 8.0


# Session variables

sort_buffer_size               = 2M                                  # Could be too big for many small sorts
tmp_table_size                 = 32M                                 # Make sure your temporary results do NOT contain BLOB/TEXT attributes

read_buffer_size               = 128k                                # Resist to change this parameter if you do not know what you are doing
read_rnd_buffer_size           = 256k                                # Resist to change this parameter if you do not know what you are doing
join_buffer_size               = 128k                                # Resist to change this parameter if you do not know what you are doing


# Other buffers and caches

table_definition_cache         = 1400                                # As big as many tables you have
table_open_cache               = 2000                                # connections x tables/connection (~2)
table_open_cache_instances     = 16                                  # New default in 5.7


# MySQL error log

log_error                      = %INSTANCEDIR%/log/%UNAME%_%INSTANCE%_error.log   # Adjust AppArmor configuration: /etc/apparmor.d/local/usr.sbin.mysqld
# log_timestamps                 = SYSTEM                              # MySQL 5.7, equivalent to old behaviour
log_warnings                   = 2                                   # MySQL 5.6, equivalent to log_error_verbosity = 3
# log_error_verbosity            = 3                                   # MySQL 5.7, equivalent to log_warnings = 2, MariaDB does NOT support this!
innodb_print_all_deadlocks     = 1
# wsrep_log_conflicts            = 1                                   # for Galera only!


# Slow Query Log

slow_query_log_file            = %INSTANCEDIR%/log/%UNAME%_%INSTANCE%_slow.log   # Adjust AppArmor configuration: /etc/apparmor.d/local/usr.sbin.mysqld
slow_query_log                 = 0
log_queries_not_using_indexes  = 0                                   # Interesting on developer systems!
long_query_time                = 0.5
min_examined_row_limit         = 100


# General Query Log

general_log_file               = %INSTANCEDIR%/log/%UNAME%_%INSTANCE%_general.log   # Adjust AppArmor configuration: /etc/apparmor.d/local/usr.sbin.mysqld
general_log                    = 0


# Performance Schema

# performance_schema             = ON                                  # for MariaDB 10 releases
performance_schema_consumer_events_statements_history_long = ON      # MySQL 5.6/MariaDB 10 and newer


# Binary logging and Replication

server_id                      = %SERVERID%                            # Must be set on MySQL 5.7 and newer if binary log is enabled!
log_bin                        = %INSTANCEDIR%/binlog/%UNAME%_%INSTANCE%_binlog            # Locate outside of datadir, adjust AppArmor configuration: /etc/apparmor.d/local/usr.sbin.mysqld
# master_verify_checksum         = ON                                  # MySQL 5.6 / MariaDB 10.2
# binlog_cache_size              = 1M                                    # For each connection!
# binlog_stmt_cache_size         = 1M                                    # For each connection!
max_binlog_size                = 128M                                # Make bigger for high traffic to reduce number of files
sync_binlog                    = 1                                   # Set to 0 or higher to get better write performance, default since MySQL 5.7
expire_logs_days               = 5                                   # We will survive Easter holidays
# binlog_expire_logs_seconds     = 432000                              # MySQL 8.0, 5 days * 86400 seconds
binlog_format                  = ROW                                 # Use MIXED if you want to experience some troubles, default since MySQL 5.7, MariaDB default is MIXED
# binlog_row_image               = MINIMAL                             # Since 5.6, MariaDB 10.1
# auto_increment_increment       = 2                                   # For Master/Master set-ups use 2 for both nodes
# auto_increment_offset          = 1                                   # For Master/Master set-ups use 1 and 2


# Slave variables

log_slave_updates              = 1                                   # Use if Slave is used for Backup and PiTR, default since MySQL 8.0
read_only                      = 0                                   # Set to 1 to prevent writes on Slave
# super_read_only                = 0                                   # Set to 1 to prevent writes on Slave for users with SUPER privilege. Since 5.7, not in MariaDB
# skip_slave_start               = 1                                   # To avoid start of Slave thread
# relay_log                      = %UNAME%_%INSTANCE%_relay-bin
# relay_log_info_repository      = TABLE                               # MySQL 5.6, default since MySQL 8.0, MySQL only
# master_info_repository         = TABLE                               # MySQL 5.6, default since MySQL 8.0, MySQL only
# slave_load_tmpdir              = '%INSTANCEDIR%/tmp'                 # defaults to tmpdir


# Crash-safe replication Master

# binlog_checksum                = CRC32                               # default
# sync_binlog                    = 1                                   # default since 5.7.6, but slow!
# innodb_support_xa              = 1                                   # default, depracted since 5.7.10


# Crash-safe replication Slave

# relay_log_info_repository      = TABLE                               # MySQL 5.6, default since MySQL 8.0, MySQL only
# master_info_repository         = TABLE                               # MySQL 5.6, default since MySQL 8.0, MySQL only
# relay_log_recovery             = 1
# sync_relay_log_info            = 1                                   # default 10000
# relay_log_purge                = 1                                   # default
# slave_sql_verify_checksum      = 1                                   # default


# GTID replication

# gtid_mode                        = ON                                  # MySQL only, Master and Slave
# enforce_gtid_consistency         = 1                                   # MySQL only, Master and Slave

# log_bin                          = %INSTANCEDIR%/binlog/%UNAME%_%INSTANCE%_binlog   # In 5.6 also on Slave
# log_slave_updates                = 1                                   # In 5.6 also on Slave


# Security variables

# local_infile                   = 0                                   # If you are security aware
# secure_auth                    = 1                                   # If you are security aware
# sql_mode                       = TRADITIONAL,ONLY_FULL_GROUP_BY,NO_ENGINE_SUBSTITUTION,STRICT_ALL_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER   # Be careful changing this afterwards
# skip_name_resolve              = 0                                   # Set to 1 if you do not trust your DNS or experience problems
# secure_file_priv               = '%INSTANCEDIR%/tmp'                   # chmod 750, adjust AppArmor configuration: /etc/apparmor.d/local/usr.sbin.mysqld


# MyISAM variables

key_buffer_size                = 8M                                  # Set to 25 - 33 % of RAM if you still use MyISAM
myisam_recover_options         = 'BACKUP,FORCE'
# disabled_storage_engines       = 'MyISAM,MEMORY'                     # MySQL 5.7, do NOT during/before mysql_upgrade, good for Galera!


# MEMORY variables

max_heap_table_size            = 64M                                 # Should be greater or equal to tmp_table_size


# InnoDB variables

innodb_strict_mode             = ON                                  # Default since MySQL 5.7, and MariaDB 10.4
innodb_buffer_pool_size        = 128M                                # Go up to 75% of your available RAM
innodb_buffer_pool_instances   = 8                                   # Bigger if huge InnoDB Buffer Pool or high concurrency

innodb_file_per_table          = 1                                   # Is the recommended way nowadays
# innodb_flush_method            = O_DIRECT                            # O_DIRECT is sometimes better for direct attached storage
# innodb_write_io_threads        = 8                                   # If you have a strong I/O system or SSD
# innodb_read_io_threads         = 8                                   # If you have a strong I/O system or SSD
# innodb_io_capacity             = 1000                                # If you have a strong I/O system or SSD

innodb_flush_log_at_trx_commit = 2                                   # 1 for durability, 0 or 2 for performance
innodb_log_buffer_size         = 16M                                 # Bigger if innodb_flush_log_at_trx_commit = 0
innodb_log_file_size           = 256M                                # Bigger means more write throughput but longer recovery time

                                                                     # Since MariaDB 10.0 and MySQL 5.6
innodb_monitor_enable = all                                          # Overhead < 1% according to PeterZ/Percona


# Galera specific MySQL parameter

# default_storage_engine         = InnoDB                            # Galera only works with InnoDB
# innodb_flush_log_at_trx_commit = 2                                 # Durability is achieved by committing to the Group
# innodb_autoinc_lock_mode       = 2                                 # For parallel applying
# binlog_format                  = row                               # Galera only works with RBR
# query_cache_type               = 0                                 # Use QC with Galera only in a Master/Slave set-up, removed in MySQL 8.0
# query_cache_size               = 0                                 # removed in MySQL 8.0
# log_slave_updates              = ON                                # Must be enabled on ALL Galera nodes if binary log is enabled!
# server_id                      = ...                               # Should be equal on all Galera nodes according to Codership CTO if binary log is enabled.


# WSREP parameter

# wsrep_on                       = on                                  # Only MariaDB >= 10.1
# wsrep_provider                 = /usr/lib/galera/libgalera_smm.so    # Location of Galera Plugin on Ubuntu ?
# wsrep_provider                 = /usr/lib64/galera-3/libgalera_smm.so   # Location of Galera v3 Plugin on CentOS 7
# wsrep_provider                 = /usr/lib64/galera-4/libgalera_smm.so   # Location of Galera v4 Plugin on CentOS 7
# wsrep_provider_options         = 'gcache.size = 1G'                  # Depends on you workload, WS kept for IST
# wsrep_provider_options         = 'gcache.recover = on'               # Since 3.19, tries to avoid SST after crash

# wsrep_cluster_name             = "My cool Galera Cluster"            # Same Cluster name for all nodes
# wsrep_cluster_address          = "gcomm://192.168.0.1,192.168.0.2,192.168.0.3"   # Start other nodes like this

# wsrep_node_name                = "Node A"                            # Unique node name
# wsrep_node_address             = 192.168.0.1                         # Our address where replication is done
# wsrep_node_incoming_address    = 10.0.0.1                            # Our external interface where application comes from
# wsrep_sync_wait                = 1                                   # If you need realy full-synchronous replication (Galera 3.6 and newer)
# wsrep_slave_threads            = 16                                  # 4 - 8 per core, not more than wsrep_cert_deps_distance

# wsrep_sst_method               = rsync                               # SST method (initial full sync): mysqldump, rsync, rsync_wan, xtrabackup-v2
# wsrep_sst_auth                 = sst:secret                          # Username/password for sst user
# wsrep_sst_receive_address      = 192.168.2.1                         # Our address where to receive SST


# Group Replication parameter

# default_storage_engine         = InnoDB                              # Group Replication only works with InnoDB
# server_id                      = %SERVERID%                          # Should be different on all 3 nodes
# log_bin                        = %INSTANCEDIR%/binlog/%UNAME%_%INSTANCE%_binlog   # Locate outside of datadir, adjust AppArmor configuration: /etc/apparmor.d/local/usr.sbin.mysqld
# binlog_format                  = ROW
# binlog_checksum                = NONE                                # not default!
# gtid_mode                      = ON
# enforce_gtid_consistency       = ON
# master_info_repository         = TABLE
# relay_log_info_repository      = TABLE
# log_slave_updates              = ON

# slave_parallel_workers         = <n>                                 # 1-2/core, max. 10
# slave_preserve_commit_order    = ON
# slave_parallel_type            = LOGICAL_CLOCK

# transaction_write_set_extraction            = XXHASH64

# loose-group_replication_group_name          = "$(uuidgen)"           # Must be the same on all nodes
# loose-group_replication_start_on_boot       = OFF
# loose-group_replication_local_address       = "192.168.0.1"
# loose-group_replication_group_seeds         = "192.168.0.1,192.168.0.2,192.168.0.3"   # All nodes of Cluster
# loose-group_replication_bootstrap_group     = OFF
# loose-group_replication_single_primary_mode = FALSE                  # = multi-primary
```







