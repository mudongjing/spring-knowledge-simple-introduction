## Spring Data ElasticSearch

首先，ElasticSearch本身作为一种搜索引擎，有那么一点像数据库，因为它自己需要维护一份信息表，以方便自己对信息进行检索；其次，由于数据量大，为分担压力，ElasticSearch有配备了集群机制，这明显像Redis。

因此，读者如果之前觉得ElasticSearch特别陌生，那么至少之前基本上是知道mysql和redis的，而这次的这个搜索引擎则相当于mysql和redis的集合体，只不过专注于字眼的提取。

那么，此时我们又发现ElasticSearch似乎有不少版本，在我这个时间上，已经有8.0了。但是，6.8版本还健在，可见这一版本的使用非常广泛，因为大的项目一般也不会那么轻易地修改自己的代码，但是我们最好还是学习较新的，毕竟新的才是未来，旧的顺便了解。但8.0过于新了，很多配套的依赖可能都还不支持，因此，为了环境配置方便，当前Spring Data最新版本为4.2，支持到7.10.0，我们就选择这个版本。

### 1. 安装

官网上已经针对不同环境都有具体的安装方法介绍了，读者可以直接参考，由于现在可以使用Docker进行安装，如果有安装Docker的话，还是使用Docker进行这些软件的管理，官网同样有介绍。【另外需要注意的是，7之后的版本要求jdk至少是11，如果本地是1.8 的，则需要使用6.8版本】~~（虽然常说1.8是主流，但11已经是必然的趋势，读者还是尽量尝试jdk11）~~

> ~~我本来是放在一个linux上运行的，读者也可以尝试着在一些云平台租用一台linux机器。为了方便，读者可以直接安装openjdk，在功能上和oracle的是没什么区别的，最多是性能上差点，但个人用也根本察觉不出。一般情况下，linux的一些发行版会事先安装上openjdk8 的版本，不必删除。~~
>
> ~~先安装我们的openjdk11,`sudo yum install java-11-openjdk `，没有yum就安装一下。~~
>
> ~~现在机器上有两个jdk，使用` alternatives --config java`可显示当前本机具备的各个jdk，输入对应版本的序号，就可以将当前使用的jdk转换为我们指定的版本了。~~

拉取elasticsearch镜像

```basic
docker pull docker.elastic.co/elasticsearch/elasticsearch:7.10.0
```

```basic
elasticsearch的文件内容
.                                                                                          ├── bin                                                                                   ├── config                                                                               ├── data                                                                                 ├── jdk                                                                                   ├── lib                                                                                   ├── LICENSE.txt                                                                           ├── logs                                                                                 ├── modules                                                                               ├── NOTICE.txt                                                                           ├── plugins                                                                               └── README.asciidoc  
```

> 如果实在是不想额外装一个jdk，安装的elasticsearch对应的目录下有一个子目录`jdk`，内部就包含了一个适合当前版本的jdk。为了使用这个自带的jdk，需要在bin目录中的`elasticsearch-env`文件中说明一下jdk的位置。类似我们安装jdk时，需要指定环境变量，在该文件中添加 `export JAVA_HOME=jdk目录的路径` 即可。

上述的命令就可以下载一个对应版本的镜像文件。如果你想删除某个镜像，可以使用下述的命令

```basic
docker image ls #查看当前本地存在的所有镜像
				#或使用 `docker images`有同样的效果
				#结果会显示出一列镜像的ID `IMAGE ID`
docker rmi 对应的id值 	  #因为id值通常很长，只要你输入的前几位保证是唯一的，即可
						#上述的删除命令也可以用`docker image rm id值`
						#当然了，之前列出的结果，还有的列有`REPOSITORY`和`TAG`.
						#此时，也可以使用`docker rmi REPOSITORY名:TAG值`删除镜像 
```

顺便再把可视化的工具`kibana`也安装了吧.~~【但我觉得我们只是想操作数据，可视化对我们而言不是太必要，同样有此想法的就可以不必安装kibana】~~

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

一般情况下，读者到这里运行就会失败了，当然你的电脑内存比较大，就可以忽略，因为默认分配给elasticsearch的内存空间是1G或2G，我这里的版本是1G,而我的机器总共也就1G~~(穷)~~ ,因此需要修改一下分配的内存大小，在config/jvm.options文件中，

```basic
-Xms512m #这里我调整为512M
-Xmx512m
```

修改也不能太小，否则根本用，总之还是需要自己试一下，看看对方够不够，以及自己能不能。

----------------

对于我不幸的是，window安装docker总是直接安装到C盘，因此我只要linux机器安装了docker，而本机是没有的，但linux机器的内存太小无法运行elasticsearch，不得不在本机直接安装。【其实所谓的安装，也只是把下载的压缩包解压就行了】

与docker启动不同的是，我需要到bin目录下启动对应的文件，如果读者去看一下bin目录下的内容，会发现很多文件是重复的，只不过一个没有后缀名，一个有bat后缀，因为bat才是windows下可以运行的，没有后缀的是linux下运行的。

而config目录下的elasticsearch.yml文件更是包含了关于elasticsearch的各种运行配置，如绑定的ip，端口，集群名和节点名等。如果在这里配置好，上述的docker命令也可以省去一部分。

> 另外一点，elasticsearch 的运行是不能使用root或管理者权限，因为太危险了。因此需要我们创建一个普通的用户，并将elasticsearch的文件所属用户设置为我们创建的用户，读者可以查询一下linux中如何创建用户以及用户组，简单的命令基本就可以了。
>
> windows下，在桌面上的计算机图标上的右键有管理，点击进去，就可以发现本地用户和组。读者可以在其中新键自己需要的组和用户。比如我建立了一个组名为 `ElasticSearchGroup` ,又创建一个用户名为`elasticuser`。

进入命令行窗口后，可以使用 `dir /q`查看一下当前文件的所属用户。如果是linux，则可以使用命令`ls -l`或`ll`查看。

 虽然可以使用命令行修改文件的所有者，但不如直接就在对应的文件夹上的属性中修改一下，linux修改这种属性直接用chown命令就可以。

此时运行elasticsearch.bat即可，在浏览器中输入`localhost:9200`即可获得一个json格式的信息。

### 2. 配置

虽然我们以及可以运行了，但是为了以后我们能够自己修改相关设置，我们再显示的配置一遍相关的端口，ip等信息。

