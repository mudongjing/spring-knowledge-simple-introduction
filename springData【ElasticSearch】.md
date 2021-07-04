## Spring Data ElasticSearch

首先，ElasticSearch本身作为一种搜索引擎，有那么一点像数据库，因为它自己需要维护一份信息表，以方便自己对信息进行检索；其次，由于数据量大，为分担压力，ElasticSearch又配备了集群机制，这明显像Redis。

因此，读者如果之前觉得ElasticSearch特别陌生，那么至少之前基本上是知道mysql和redis的，而这次的这个搜索引擎则相当于mysql和redis的集合体，只不过专注于字眼的提取。

那么，此时我们又发现ElasticSearch似乎有不少版本，在我这个时间上，已经有8.0了，稳定版的是7.13.2。但是，6.8版本还健在，可见这一版本的使用非常广泛，因为大的项目一般也不会那么轻易地修改自己的代码，但是我们最好还是学习较新的，毕竟新的才是未来，旧的顺便了解。但8.0过于新了，很多配套的依赖可能都还不支持，因此，为了环境配置方便，当前Spring Data最新版本为4.2，支持到7.10.0，我们就选择这个版本。

### 1. 安装

官网上已经针对不同环境都有具体的安装方法介绍了，读者可以直接参考，由于现在可以使用Docker进行安装，如果有安装Docker的话，还是使用Docker进行这些软件的管理，官网同样有介绍。【另外需要注意的是，7之后的版本要求jdk至少是11，如果本地是1.8 的，则需要降一降，使用6.8版本】*~~（虽然常说1.8是主流，但11已经是必然的趋势，读者还是尽量尝试jdk11）~~*

> ~~我本来是放在一个linux上运行的，读者也可以尝试着在一些云平台租用一台linux机器。为了方便，读者可以直接安装openjdk，在功能上和oracle的是没什么区别的，最多是性能上差点，但个人用也根本察觉不出。一般情况下，linux的一些发行版会事先安装上openjdk8 的版本，不必删除。~~
>
> ~~先安装我们的openjdk11,`sudo yum install java-11-openjdk `，没有yum就安装一下。~~
>
> ~~现在机器上有两个jdk，使用` alternatives --config java`可显示当前本机具备的各个jdk，输入对应版本的序号，就可以将当前使用的jdk转换为我们指定的版本了。~~

拉取elasticsearch镜像

```basic
docker pull docker.elastic.co/elasticsearch/elasticsearch:7.10.0
```

上述的命令就可以下载一个对应版本的镜像文件。

elasticsearch的文件内容，

```basic
.                                                                                          ├── bin                                                                                   ├── config                                                                               ├── data                                                                                 ├── jdk                                                                                   ├── lib                                                                                   ├── LICENSE.txt                                                                           ├── logs                                                                                 ├── modules                                                                               ├── NOTICE.txt                                                                           ├── plugins                                                                               └── README.asciidoc  
```

> 如果实在是不想额外装一个jdk，安装的elasticsearch对应的目录下有一个子目录`jdk`，内部就包含了一个适合当前版本的jdk。为了使用这个自带的jdk，需要在bin目录中的`elasticsearch-env`文件中说明一下jdk的位置。类似我们安装jdk时，需要指定环境变量，在该文件中添加 `export JAVA_HOME=jdk目录的路径` 即可。

如果你想删除某个镜像，可以使用下述的命令

```basic
docker image ls #查看当前本地存在的所有镜像
				#或使用 `docker images`有同样的效果
				#结果会显示出一列镜像的ID `IMAGE ID`
docker rmi 对应的id值 	  #因为id值通常很长，只要你输入的前几位保证是唯一的，即可
						#上述的删除命令也可以用`docker image rm id值`
						#当然了，之前列出的结果，还有的列有`REPOSITORY`和`TAG`.
						#此时，也可以使用`docker rmi REPOSITORY名:TAG值`删除镜像 
```

顺便再把可视化的工具`kibana`也安装了吧.~~【*但我觉得我们只是想操作数据，可视化对我们而言不是太必要，同样有此想法的就可以不必安装kibana*】~~

```basic
docker pull docker.elastic.co/kibana/kibana:7.10.0
```

> 镜像就相当于磁盘上的程序，是一串固定的内容，而Docker中的容器则相当于运行起来的进程。

docker运行elasticsearch

```basic
docker run -d --name es -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.10.0
# 此时才是真正创建了一个容器
#-d 表示以后台守护进程形式运行
#--name 指为该容器指定了一个名字，以后对该容器的操作就可以使用这个名字替代
# -p指定端口，第一个是http的端口，也就是我们能够访问的，第二个是集群内的交流端口，是机器之间互相访问用的端口
# discovery.type=single-node 说明此时是以单节点运行
# 最后指明对应的镜像
```

一般情况下，读者到这里运行可能会失败了。

**失败调整**

- 因为默认分配给elasticsearch的内存空间是1G或2G，我这里的版本是1G,而我的机器总共也就1G~~(穷)~~ ,因此需要修改一下分配的内存大小，在config/jvm.options文件中，【如果内存足够大可以忽略】

```basic
-Xms512m #这里我调整为512M
-Xmx512m
```

​	修改也不能太小，否则根本不够用，总之还是需要自己试一下，看看对方够不够，以及自己能不能。

- 另外，在linux上还存在一些小问题，如启动时显示线程数量小了或文件描述符小了，这些直接可以在`/etc/security/limit .conf`文件中说明。

```txt
所有者名 soft nofile 65535
所有者名 hard nofile 65535 #这两条设置最大的文件描述符
所有者名 soft nproc 4096
所有者名 hard nproc 4096  # 这两条设置最大的可创建的线程数
```

- 另外还有需要扩充虚拟内存，在文件`/etc/sysctl.conf`

```txt
vm.max_map_count=262144
#修改完之后，再用命令 `sysctl -p`刷新一下
```

- 最后，如果我们试图配置一个集群，需要配备一个集群节点的入口，就好像用种子下载文件，终究还是需要一个服务器告诉我们种子的信息和对应资源的信息。这里则需要让集群中的节点有个主心骨。愿意的话只需要配置下述3个中的一个，在elasticsearch的config/elasticsearch.yml文件中配置，

```basic
discovery.seed_hosts:后跟一个列表，内含节点的ip:端口，端口可以不写，默认是我们配置的内部交流端口
discovery.seed_providers:和上述是同样的功能，只不过这个是跟的一个文件，可在一个文件中写上节点的ip和端口
cluster.initial_master_nodes:这里不仅可以写ip:端口，也可以直接写节点的名字，主要是说明主节点
```

- 如果显示java的回收器可能将废弃，也可以在jvm.options文件中修改。不过可以忽略。

<font color=blue>+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++</font>

<font color=blue>+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++</font>

对于我不幸的是，window安装docker总是直接安装到C盘~~【*我非常不喜欢这样*】~~，因此我只有linux机器安装了docker，而本机是没有的，但linux机器的内存太小(~穷~)无法运行elasticsearch，不得不在本机直接安装。【其实所谓的安装，也只是把下载的压缩包解压就行了】

与docker启动不同的是，我需要到bin目录下启动对应的文件，如果读者去看一下bin目录下的内容，会发现很多文件是重复的，只不过一个没有后缀名，一个有bat后缀，因为bat才是windows下可以运行的，没有后缀的是方便命令行操作的。

而config目录下的elasticsearch.yml文件更是包含了关于elasticsearch的各种运行配置，如绑定的ip，端口，集群名和节点名等。如果在这里配置好，上述的docker命令也可以省去一部分。

> 另外一点，elasticsearch 的运行是不能使用root或管理者权限，因为太危险了。因此需要我们创建一个普通的用户，并将elasticsearch的文件所属用户设置为我们创建的用户，读者可以查询一下linux中如何创建用户以及用户组，简单的命令基本就可以了。
>
> windows下，在桌面上的计算机图标上的右键有管理，点击进去，就可以发现本地用户和组。读者可以在其中新键自己需要的组和用户。比如我建立了一个组名为 `ElasticSearchGroup` ,又创建一个用户名为`elasticuser`。

进入命令行窗口后，可以使用 `dir /q`查看一下当前文件的所属用户。如果是linux，则可以使用命令`ls -l`或`ll`查看。

 虽然可以使用命令行修改文件的所有者，但不如直接就在对应的文件夹上的属性中修改一下，linux修改这种属性直接用chown命令就可以。

此时运行elasticsearch.bat即可，或在命令行中输入`elasticsearch`亦可，在浏览器中输入`localhost:9200`即可获得一个json格式的信息。

### 2. 配置

- elasticsearch.yml文件

  虽然我们以及可以运行了，但是为了以后我们能够自己修改相关设置，我们再显式地配置一遍相关的端口，ip等信息。

  我们简化一下elasticsearch.yml文件，如下【不同版本的yml文件内容可能有所区别】

```basic
#cluster.name: my-application 如果是多个机器组成集群，则需要指定它们的一个集群的名字
# node.name: node-1 对应的自己作为集群的一员也需要一个名字
# 只不过，我们现在还不需要建立集群，因此这些功能就处于注释状态，冒号前面的名字别乱改，后面可以随便写
# 如果不希望将设置写在文件中，也可以在启动命令上 为每个属性前 加上-E，在用=跟上指定的值

#因为参与的节点在功能上有区别，集群中的节点还需要标明各节点的功能作用等
#node:
#	master:true 有资格竞选主节点
#	data:true 本地将负责存储数据
#	以及其它各种性质

path:
	data:存放数据的目录路径
	logs: 日志文件的路径

network.host: 0.0.0.0 
# 用于指定哪些ip可以远程访问我们，如果设置0.0.0.0则表示所有ip，默认是_local_,即127.0.0.1
#如果确定了一个工作环境，那么可以直接访问的节点ip也就那几个

http:
	port: 9200#我们可以直接放的端口
	#bind_host:#机器绑定的ip
#network.bind_host:这也是一个绑定ip的，它是说明该节点从哪个地址监听请求的
#总之，我们现在还不需要设置这些ip,一般也不太用设置太复杂，
#如果使用集群了，需要说明一下节点在局域网中的地址即可
#network.publish_host:这是在集群中发布自身的ip值，这个只能写一个ip
#之所以bind_host可以写多个，是因为存在内网和外网环境，
#不同用户可能处于不同的网络环境，因此使用的对应的ip是不同的
#而节点之间是处于同一环境，为保证各节点的唯一性，需明确各节点对应的ip值

transport.port:9300 #这里就是负责节点之间通信的端口，并且可以是一个端口范围，默认是9300-9400

#discovery.seed_hosts: ["host1", "host2"]
#cluster.initial_master_nodes: ["node-1", "node-2"] 这两条就是集群的入口
#从示例就可以明显可出是对应的其它的节点的ip或名字，
#集群中的每个节点都包含这些关于节点的信息，否则启动时怎么互相发现对方
#即使，我们现在是单节点的状况，也需要指明这些信息
discovery.seed_hosts: []
cluster.initial_master_nodes: [] 
#但是情况所限，我们现在需要的是单节点启动，这两条语句中的[]中就不写任何东西，让它空着

#gateway.recover_after_nodes: 3 这里这是要求，集群至少得有3个节点

# Require explicit names when deleting indices:

http.cors.enabled:true
http.cors.allow-orgin:"*" #这两条用来说明第三方插件可以请求该节点或集群
```

​	上述介绍了基本的一些设置。更具体的配置细节可参考[官方文档]([Configuring Elasticsearch | Elasticsearch Guide [7.10\] | Elastic](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/settings.html))。

- head插件【别放在plugins目录中】

  另外为了前期我们能够可视化的操作elasticsearch，我们需要一个插件，帮助我们在浏览器中进行各种动作并显示结果，常见的教程会告诉我们可以安装一个head插件，其中就是将github上head项目下载下来，然后再命令行中定位到对应的目录中，使用命令`npm install`进行安装，再`npm run start`启动它，如果npm启动有些许问题，可以安装cnpm，这其实就是npm的中国版，用法基本一致，在运行前需要在其中的文件 `Gruntfile.js`中 的server选项中添加 `hostname`

```js
connect: {
			server: {
				options: {
					hostname: '*',//这是我们需要额外添加的
					port: 9100,
					base: '.',
					keepalive: true
				}
			}
		}
```

​	head插件可以放在elasticsearch的plugins目录中（但可能会报错），也可随便放在其它位置（尽量别放在elasticsearch的其它位置，以免启动有问题）。

​	**但是我们作为后端，终究不是和可视化打交道的,没必要浪费时间在这些可视化上，head插件的功能可以直接使用chrome商店中的插件`ElasticSearch Head `来替代**。此外可视化也不是head一家，其它有一些好看的，但对于我们而言，之后的java操作，我们根本就不想看它们。

- ik中文分词插件

  最后，作为中文用户，不幸的一点在于用的这些框架大多都是外国人写的，天然得对中文的支持非常差。因此需要额外安装一个`ik`插件，以帮助进行中文的检索，项目位于github上[ elasticsearch-analysis-ik](https://github.com/medcl/elasticsearch-analysis-ik)。同样的，将项目本身下载到本地，解压即可，注意下载对应版本【作者基本上跟着官方的脚步同步更新，这里需要感谢作者的付出】。将下载好的对应版本的zip文件放到elasticsearch的plugins下解压即可，注意把zip文件删掉。

--------------

现在，我们已经可以用ElasticSearch，但是，对于很多读者而言，在接触ES之前大多数应该是接触过并使用了一段时间的数据库，因此我们可能已经有一些数据放在了原本的mysql之类的数据库中，我们可能希望那这些数据来练手，而且真正的使用中，我们仍然需要使用类似关系型数据库来保存数据，但是同时需要将数据同步到ES中进行搜索。于是，我们特别需要一个东西来协助我们将数据库的信息同步到ES中，这就是logstash需要做的工作，它的功能就是采集数据，不论是关系型的mysql，还是非关系型的redis，它都支持。

首先还是下载，解压。不必放在elastisearch的目录下，这类需要我们自己启动的软件都是可以随便放在其它地方。

然后在对应的config目录中创建一个`jdbc.conf`的文件，用于指定数据源和目的地。如果我们是打算从mysql中拿数据，就需要额外在bin目录中添加一个mysql-connector-java的jar包。而conf文件大致的内容如下：

```bash
input {
  jdbc {
      jdbc_connection_string => "jdbc:mysql://mysql的ip:3306/表名?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC" 
      #这里连接我们的数据库，后面的时区是8版本之后必须的
      jdbc_user => "用户名"
      jdbc_password => "密码"
      jdbc_driver_library => "mysql-connector-java的jar包路径"#指定驱动文件路径
      jdbc_driver_class => "com.mysql.jdbc.Driver"#这是8版本之前的，之后的需要mysql.后加上cj
      jdbc_validate_connection => "true"#是否在使用前验证连接的可用性，默认是false
      
      jdbc_paging_enabled => "true"#是否分页
      jdbc_page_size => "1000"#每一页的数量
      jdbc_default_timezone => "Asia/Shanghai"#时区
      #statement => "sql 语句" 直接执行sql语句
      statement_filepath => "sql文件"#执行sql文件，内部的命令就是提取sql中的数据，以同步到ES中
      								#主要是select语句，以提取数据

      #设置监听间隔  各字段含义（由左至右）分、时、天、月、年，全部为*默认含义为每分钟都更新
      schedule => "* * * * *"#这是每分钟执行依此我们指定的sql语句或文件
      #schedule => "*/10 * * * *" 每隔10分钟执行一次
      lowercase_column_names => false #不许将数据库中的列名转化为对应的小写
      
      use_column_value => true#此时需要指定一个列名是递增的， tracking_column即指定对应的列
      #如果不需要递增，就没必要设置这个参数
      tracking_column_type => "numeric" #递增字段的类型，numeric 表示数值类型
      tracking_column => "递增的列名"#一般就是值我们设置的哪个主键
      
      record_last_run => true#记录上次执行结果, 把上次执行到的 tracking_column 字段的								  值记录下来,保存到last_run_metadata_path
      last_run_metadata_path => "指定一个txt文件路径"#记录最新的同步的offset信息

      clean_run => false#不清除 last_run_metadata_path 的记录,如果为真那么每次都相当于从头开始查询																			所有的数据库记录

      # 索引类型
      #type => "jdbc"
    }
    #如果你要导入多个表，多写几个jdbc{}就行了
}

output {
  elasticsearch {
        hosts => ["http://localhost:9200"]#ES的ip和端口
        index => "随便写个索引名称，用于存储即将导入的数据"#需要时你已经设置好的一个索引值
        document_type => "_doc"#文档类型
        document_id => "%{数据库的一个列名}" #我们指定的sql语句或文件按指定频率定期执行
        								  #获得的结果中，我们指定的那个列名的值将纳入ES中
    }
    stdout {
        codec => json_lines
    }
}
```

准备好后，到bin目录中使用启动命令：

```bash
logstash -f jdbc.conf的路径 -t #-t表示先测试一下，如果成功，再去掉-t，正式运行
```

如果jdbc.conf不好加载，可以试一试放在其它位置上，比如放在bin目录中。



### 3.基本概念

- 前文已经提到过，ElasticSearch相当于Redis和Mysql的集合体，那么既然相当于Mysql，自然需要存储数据，elasticsearch的存储结构是，index（索引库），内部包含type（类型）【但是7之后，这个type现在一个索引就一个，因此要不要它已经无所谓】，再里面就是document（文档）【相当于一条记录】，最后是field（字段）【就是记录中的各种属性，相当于数据库中表的列】。

- 我们在一个节点上可以创建若干个分片，并规定副本数，若是一个副本，则集群中，在其它某个节点上存在一个拷贝。

  我们是看不出分片的存在的，通过将数据放在分片中，我们只需要知道如何计算一个数据存放的分片标号，分片具体的存放位置就可以灵活一些，方便在集群中存放数据。

- 而索引则相当于一个数据库，我们访问数据就是需要指明对应的索引，分片是一个数据容器，索引是个抽象的概念，我们明面上在一个索引中操作数据，但实际上，数据会按照指定的规则存放到对应的分片中，类似于Redis中的数据槽。

- 一个索引的内容可能存放在不同的分片上，但其中的一个文档则必然存储在一个分片内。节点的分片数量是我们可以指定的，但指定之后就不要修改了，否则之前的信息全部错乱，数据就无法找到了。【大致上，数据也是按照计算哈希值，在对当前节点的分片数量求模，得到需要存放内容的分片号】。

- 另外，为什么elasticsearch就可以快速地进行词语的搜索。就是使用了倒排索引，又称为反向索引，简单说，我们平常看文章，是看到文章的第几行第几个字是什么字，而倒排则是整理一遍文章得出，某个字都在哪些行哪个地方出现过。因此，当需要搜索某些词汇时，可以第一时间得到存在指定词汇的内容，然后在对得到的效果按照相关度等标准计算结果的得分，按照得分进行排列。

### 4. ES的基本操作

前面已经明确了如何启动ES，而如何关闭它则可以又多种方法，你可以暴力地直接kill对应的进程，有head插件的话，也可以在页面上找找动作中是否有关停的选项。6.8以前还有_shutdown的API，可以通过http传入命令，但已经被废弃了。



### 5. Java连接

我们直接这里使用IDEA建立了项目后，直接在test目录中书写代码，方便直接运行。

需要的依赖主要有，

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.11</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.elasticsearch</groupId>
    <artifactId>elasticsearch</artifactId>
    <version>7.10.0</version>
</dependency>
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-client</artifactId>
    <version>7.10.0</version>
</dependency>
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-high-level-client</artifactId>
    <version>7.10.0</version>
</dependency>
```

测试用的代码，

```java
private RestHighLevelClient restHighLevelClient=null;//与ES的连接
    private final String SCHEME="HTTP";
    HttpHost[] httpHosts=new HttpHost[]{
            new HttpHost("你的ip",9200,SCHEME)};

    @Before
    public void init(){//开启连接
        restHighLevelClient=new RestHighLevelClient(RestClient.builder(httpHosts));
    }
    @After
    public void close() {//关闭连接
        if(restHighLevelClient!=null){
            try {
                restHighLevelClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
	//我们首先在ES中创建一个索引`sy`,之后的数据主要放在这里，因为是单节点，所以设置副本数量为0
    //添加记录，如果指定的索引是不存在的，则会自动创建一个索引，副本是1，分片也是1
	//如果不指定id，ES会生成一个随机id，ES本身还没有支持id自增
    @Test
    public void create(){
        Map<String ,Object> map=new HashMap<>();
        map.put("name","他的名字");
        map.put("content","他写的内容");//name和content都是我们自己随便指定的
        IndexRequest indexRequest=new IndexRequest("sy").id("1").source(map);
        //上述语句将把map中的内容添加到索引sy中，对应的字段就是name和content,相当于列名
        try {
            IndexResponse response =restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);//这里正式将指令发送给ES
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //查询，若指定的索引不存在，则报异常
    @Test
    public void query(){
        GetRequest getRequest=new GetRequest("sy","1");
        //就是简单地查询id为1的记录，ES中应该称之为文档
        try {
            GetResponse getResponse=
                restHighLevelClient.get(getRequest,RequestOptions.DEFAULT);
            System.out.println(getResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //更新，与添加非常类似
    @Test
    public void update(){
        Map<String ,Object> map=new HashMap<>();
        map.put("name","他的新名字");
        map.put("content","他写的新内容");
        UpdateRequest updateRequest=new UpdateRequest("sy","1").doc(map);//更新id为1的信息
        try {
            UpdateResponse updateResponse=restHighLevelClient.update(updateRequest,
                    RequestOptions.DEFAULT);
            System.out.println(updateRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	//删除某一记录
    @Test
    public void delete(){
        DeleteRequest deleteRequest=new DeleteRequest("sy","指定的id");
        try {
            DeleteResponse deleteResponse=restHighLevelClient.delete(deleteRequest,
                    RequestOptions.DEFAULT);
            System.out.println(deleteResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //批量操作，但这里不能批量执行查询
    @Test
    public void batch(){
        BulkRequest bulkRequest=new BulkRequest();
        bulkRequest.add(new IndexRequest("sy").id("3").source(XContentType.JSON,
                "name","我的名字","content","别人的名字"));//添加
        bulkRequest.add(new UpdateRequest("sy","3").doc(XContentType.JSON,
                "name","我的新名字"));//更新
        bulkRequest.add(new DeleteRequest("sy","2"));//删除
        try {
            BulkResponse bulkResponse= restHighLevelClient.bulk(bulkRequest,RequestOptions.DEFAULT);
            System.out.println(bulkResponse.toString()  );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //高级查询
    @Test
    public void testquery(){
        SearchRequest searchRequest = new SearchRequest("sy");//可以指定多个索引库
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder().
                                                query(QueryBuilders.matchAllQuery());
        //指定的查询是匹配所有
        //SearchSourceBuilder默认最多显示10条结果，可以通过size()设置，
        //也可以通过from()设置从哪个索引开始搜索
        //可以用sort()排序，是按照分数或其它，这些排序的依据可以使用类sortBuilders指定,
        //sortBuilder可以指定是正序或是倒序,更多的可以查看对应源码中的方法
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println(searchResponse);
/* ES查询返回的内容，大致是如下的json格式
            {
              "took": 3,
              "timed_out": false,//是否超时
              "_shards": {//分片情况
                "total": 5,
                "successful": 5,
                "skipped": 0,
                "failed": 0
              },
              "hits": {		//这是上级的hits
                "total": {
                  "value": 1,	//这里对应的是结果的总数
                  "relation": "eq"
                },
                "max_score": 1,
                "hits": [	//这是下级的hits，是一个数组，内部就是结果
                  {
                    "_index": "sy",
                    "_type": "_doc",
                    "_id": "3",
                    "_score": 1,
                    "_source": {
                      "name": "我的新名字",
                      "content": "别人的名字"
                    }
                  }
                ]
              }
            }
 */
            long value = searchResponse.getHits().getTotalHits().value;
            //这里的getHits就是对应的上级hits，getTotalHits对应的就是其中的结果总数
            System.out.println("记录总数"+value);
            if(value>0){
                SearchHit[] hits=searchResponse.getHits().getHits();
                //分别对应上级和下级的hits
                for(SearchHit hit:hits){
                    System.out.println("index"+hit.getIndex()+", id"+hit.getId());
                    System.out.println("name"+hit.getSourceAsMap().get("name")+
                                       ", content"+hit.getSourceAsMap().get("content"));
                }
                //因为name和content是我们自己搞得，需要额外getSourceAsMap()说明一下
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	//加一点高亮
    @Test
    public void testquerymatch(){
        SearchRequest searchRequest = new SearchRequest("sy");
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder().
                query(QueryBuilders.multiMatchQuery("别人","content"));
        //别人是我们要查找的关键词，后面可以跟多个字段，我们这里只弄了一个content
        HighlightBuilder highlightBuilder=new HighlightBuilder().field("content").preTags("<font color=yellow>").postTags("</font>");
      //上述语句负责高亮，首先指定负责的地方是content字段，preTags和postTags对应的就是html中的高亮语法
        searchSourceBuilder.highlighter(highlightBuilder);//设置高亮
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println(searchResponse);
            long value = searchResponse.getHits().getTotalHits().value;
            System.out.println("记录总数"+value);
            if(value>0){
                SearchHit[] hits=searchResponse.getHits().getHits();
                for(SearchHit hit:hits){
                    System.out.println("index"+hit.getIndex()+", id"+hit.getId());
                    System.out.println("name"+hit.getSourceAsMap().get("name")  +
                            ", content"+
                String.valueOf(hit.getHighlightFields().get("content").fragments()[0]));
                  //被高亮的字段有着自己的格式，大致就是被搞成了一个数组，这里主要是把他作为字符串提取出来
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
```







