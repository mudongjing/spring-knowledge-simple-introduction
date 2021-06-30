## Spring Data ElasticSearch

首先，ElasticSearch本身作为一种搜索引擎，有那么一点像数据库，因为它自己需要维护一份信息表，以方便自己对信息进行检索；其次，由于数据量大，为分担压力，ElasticSearch有配备了集群机制，这明显像Redis。

因此，读者如果之前觉得ElasticSearch特别陌生，那么至少之前基本上是知道mysql和redis的，而这次的这个搜索引擎则相当于mysql和redis的集合体，只不过专注于字眼的提取。

那么，此时我们又发现ElasticSearch似乎有不少版本，在我这个时间上，已经有8.0了。但是，6.8版本还健在，可见这一版本的使用非常广泛，因为大的项目一般也不会那么轻易地修改自己的代码，但是我们最好还是学习较新的，毕竟新的才是未来，旧的顺便了解。但8.0过于新了，很多配套的依赖可能都还不支持，因此，为了环境配置方便，当前Spring Data最新版本为4.2，支持到7.10.0，我们就选择这个版本。

### 1. 安装

官网上已经针对不同环境都有具体的安装方法介绍了，读者可以直接参考，由于现在可以使用Docker进行安装，我们还是使用Docker进行这些软件的管理，官网同样有介绍。

```basic
docker pull docker.elastic.co/elasticsearch/elasticsearch:7.10.0
```

上述的命令就可以下载一个对应版本的镜像文件。如果你像删除某个镜像，可以使用下述的命令

```basic
docker image ls #查看当前本地存在的所有镜像
#结果会显示出一列镜像的ID `IMAGE ID`
docker rmi 对应的id值 #因为id值通常很长，只要你输入的前几位保证是唯一的，即可
#上述的删除命令也可以用`docker image rm id值`
#当然了，之前列出的结果，还有的列有`REPOSITORY`和`TAG`.
#此时，也可以使用`docker rmi REPOSITORY名:TAG值`删除镜像 
```

