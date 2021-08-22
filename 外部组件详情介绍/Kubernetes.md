

> Kuberbetes常被称为k8s。k8s可以视为是容器平台的控制系统，现在的生产环境动不动就涉及几十个，上百个大大小小的组件，而每个组件都需要我们一个个地负责配置，后来就出现了Docker这样可以一键安装对应组件的工具，Docker则称之为容器。现在的问题是，这个的问题是，项目过大，安装方便的了，但是管理还是需要一个个操作，如果遇到一些意外状况，还必须人工去手动干预，因此k8s就是让这些容器化的组件能够按照指定的规则自行处理自身的相关事态。
>
> > 形象地说，以前我们安装软件需要一个个去官网下载，再自己安装，并指定安装位置，和是否开机启动之类的。
> >
> > 现在我们有了应用商店，直接搜索并点击即可完成对应的安装，中间确认几个选项即可完成操作。
> >
> > 现在，软件安装了，但是我们希望能让软件保持为最新版本，另外我们还有其它几台电脑，还是不同操作系统的，还有其它一些平板，手机，都希望自己的软件始终是最新的，且要求它们在更新的时候使用的是wifi流量，而且是在我不使用它们的使用，悄悄更新。
> >
> > > 那么，为了完成上述的功能，自然需要一个额外的组件，能够操纵各机器各系统的应用商店，还需要考虑各种因素是否符合要求，以做出合适的方案，实现我们希望的结果。



# 安装

> 建议读者准备一个linux系统。我这里使用的时Centos。

安装自然是可以像其它组件那样下载对应的安装包，直接解压安装再配置。

但是，需要注意的是，Kubernetes相比于其它组件而言稍显复杂，内部又包含了其它组件以及对应的配置也繁复。因此，官网也推荐在学习期间，可以使用对应的工具进行安装。

安装工具可以看一下官网的[文档](https://kubernetes.io/docs/tasks/tools/)。那里提供了各种工具，均可使用体验，我们这里就介绍其中的`kubeadm`工具，因为在生产环境做集群部署时，官方也是支持这个工具的。

## 准备工作

- 在自己的电脑上，由于k8s需要安装的时集群，自然就有主节点和从节点之分，为了方便之后的使用，我们可以在系统的hosts文件中指明其它几个机器的机器名和对应的ip地址。

  一般是在对应的`/etc/hosts`文件中加上类似`ip 机器名`的格式即可。

- 另外k8s要求不同的节点之间的时间必须保持一致，我们可以使用网络同步时间

  例如使用`chronyd`

  ```bash
  systemctl start chronyd
  systemctl enable chronyd
  ```

- 关闭防火墙。安装了k8s，我们必然还要安装注入Docker之类的容器组件，它们都会产生很多iptables规则，容易与系统的规则发生冲突，

  ```bash
  systemctl stop firewalld
  systemctl disable firewalld
  # 关闭了防火墙，当然实际的工作中，肯定不会随便就把防火墙给关了，只是如果我们自己调整，不方便后面的学习
  #下面关闭iptable服务
  systemctl stop iptables
  systemctl disable iptables
  ```

- selinux，这个也需要关掉，本质是一个安全服务，但跟很多组件都会有这样那样的冲突，

  ```bash
  # 找到/etc/selinux/config 文件，修改其中的SELINUX
  SELINUX=disabled
  ```

- 禁用swap分区，这本是作为linux的物理内存，作为虚拟内存，只不过对系统有一些负面影响，而k8s则要求机器禁用它。是在不能禁，则需要在安装的时候做对应的说明。

  ```bash
  # 编辑/etc/fstab文件
  # 如果其中发现有类似下面的语句，则注释它
  路径 swap            swap  defaults 0 0
  ```

- 内核参数。

  ```bash
  # 新键一个文件，在/etc/sysctl.d/中，创建一个kubernetes.conf文件
  # 添加配置
  net.bridge.bridge-nf-call-ip6tables = 1
  net.bridge.bridge-nf-call-iptables = 1
  net.ipv4.ip_forward = 1
  # 上述添加了网桥过滤和地址转发的功能
  # 重新加载配置
  sysctl -p
  # 加载网桥过滤模块
  modprobe br_netfilter
  # 查看加载是否都成功
  lsmod | grep br_netfilter
  ```

- 配置ipvs功能。这是一种代理模型，另外还有基于iptables的代理，只不过，这种性能更好而已，

  ```bash
  # 缺点是，我们需要额外地安装对应的模块
  yum install ipset ipvsadmin -y
  # 另外，为了加载该模块，我们需要写一个脚本文件来运行
  # 脚本名字可以写作 ipvs.modules
  # 内容是(读者注意，没有那些横杠)
  ---------------
  #! /bin/bash
  modprobe -- ip_vs
  modprobe -- ip_vs_rr
  modprobe -- ip_vs_wrr
  modprobe -- ip_vs_sh
  modprobe -- nf_conntrack_ipv4
  ---------------
  # 运行脚本即可，一般教程可能会要求修改脚本权限之类的，实际在对应的目录下，source即可
  # 比如 
  source ipvs.modules
  # 即可运行
  # 检查一下是否加载成功
  lsmod | grep -e  ip_vs -e nf_conntrack_ipv4
  ```

完成以上的工作后，需要重启服务器，以生效。

# 使用

