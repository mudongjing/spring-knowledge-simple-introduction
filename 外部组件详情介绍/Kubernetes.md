

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

之后查看一下，

```bash
getenforce
# 看返回的是否是 disabled
free -m
# 看一下swap分区是否是 0
```

## 组件安装

- 首先是容器组件，一般就是Docker，但是目前最新的1.22版本，应该是移除了Docker的支持，应该说，从1.20开始，似乎就有这个想法，建议读者使用1.19或1.18。我这里使用的是1.18版本。

  > 虽说移除了docker，但是容器化处理不是必须依赖docker的。
  >
  > 可以看一下k8s支持的底层系统，docker只是其中一个，
  >
  > ![](https://mudongjing.github.io/gallery/k8s/op/baseop.png "摘自《Kubernetes实战》")

  ​	我这里使用的docker版本是18.06.3，具体安装如下，

  ```bash
  # 首先获得docker的镜像源
  wget https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo -o /etc/yum.repos.d/docker-ce.repo
  # 现在，我们获取了docker的可用版本，查看一下版本列表
  yum list docker-ce --showduplicates
  # 选项18.06.3版本，如果是系统是centos8版本的，则没有这个版本
  # 如果愿意的话，可以直接去
  # https://mirrors.aliyun.com/docker-ce/linux/centos/7.5/x86_64/stable/Packages/
  # 下载该版本，其实其它版本也可以
  yum install --setopt=obsoletes=0 docker-ce-18.06.3.ce-3.e17 -y
  # --setopt=obsoletes=0 用以禁止yum自动安装最新版
  # 添加配置文件
  # 在/etc目录下新键一个目录 docker
  # 在里面新建一个json文件 daemon.json
  # 内容是
  ----------------------
  {
  "exec-opts" : ["native.cgroupdriver=systemd"],
  "registry-mirrors": ["https://kn0t2bca.mirror.aliyuncs.com"]//这是指明默认仓库，国情所致
  }
  ----------------------
  # 之后启动docker
  systemctl start docker
  # 看一下版本，顺便确认安装成功
  docker -v
  # 之后也可以设置为开机启动
  systemctl enable docker
  ```

- k8s

  ```bash
  # 我们还是编辑一个软件源的配置文件，指明对应的镜像源
  # 在/etc/yum.repos.d 目录下新建一个kubernetes.repo文件
  # 内容如下
  -----------------
  [kubernetes]
  name=Kubernetes
  baseurl=http://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64
  enabled=1
  gpgcheck=0
  repo_gpgcheck=0
  gpgkey=http://mirrors.aliyun.com/kubernetes/yum/doc/yum-key.gpg
  	   http://mirrors.aliyun.com/kubernetes/yum/doc/rpm-package-key.gpg
  -----------------
  # 安装
  yum install --setopt=obsoletes=0 kubeadm-1.17.4-0 kubelet-1.17.4-0 kubectl-1.17.4-0 -y
  # 然后在/etc/sysconfig/ 目录下的kubelet文件中添加以下内容
  ----------
  KUBELET_CGROUP_ARGS="--cgroup-driver=systemd"
  KUBE_PROXY_MODE="ipvs"
  ----------
  # 然后也可以将其设置为开机启动
  systemctl enable kubelet
  ```

  虽然真正意义上的安装没有完成，但是当我们使用kubeadm安装的话，其实基本需要的已经准备好了，最后就是利用kubeadm完成最后最麻烦的那部分即可。

  但是，还是限于国内网络，kubeadm需要下载的组件还是需要切换到国内的镜像，

  ```basic
  # 输入以下命令，查看一下我们需要的下载的组件
  kubeadm config images list
  # 我这里可以看到需要的组件是
  ---------
  k8s.gcr.io/kube-apiserver:v1.17.17
  k8s.gcr.io/kube-controller-manager:v1.17.17
  k8s.gcr.io/kube-scheduler:v1.17.17
  k8s.gcr.io/kube-proxy:v1.17.17
  k8s.gcr.io/pause:3.1
  k8s.gcr.io/etcd:3.4.3-0
  k8s.gcr.io/coredns:1.6.5
  ---------
  # 我们使用阿里的镜像，但是我们还需要对阿里下载的镜像做一些稍稍的修改
  # 总之，就是利用docker拉去对应的镜像，在打个标签
  # 所有的工作，我们都放在脚本中运行，随便找个地方编辑一下脚本文件，内容如下
  ------------
  #! /bin/bash 
  images=(
      kube-apiserver:v1.17.17
      kube-controller-manager:v1.17.17
      kube-scheduler:v1.17.17
      kube-proxy:v1.17.17
      pause:3.1
      etcd:3.4.3-0
      coredns:1.6.5
  )
  for imageName in ${images[@]}
  do
      docker pull registry.cn-hangzhou.aliyuncs.com/google_containers/$imageName
      docker tag registry.cn-hangzhou.aliyuncs.com/google_containers/$imageName k8s.gcr.io/$imageName
      docker rmi registry.cn-hangzhou.aliyuncs.com/google_containers/$imageName
  done
  -------------
  ```

  ```basic
  # 直接source 命令运行一下这个脚本即可安装对应的组件，当然docker需要启动起来
  # 然后，使用docker可以看一下我们下载的镜像
  docker images
  # 例如
  ---------
  REPOSITORY                                      TAG        IMAGE ID       CREATED         SIZE
  k8s.gcr.io/kube-proxy                           v1.17.17   3ef67d180564   7 months ago    117MB
  k8s.gcr.io/kube-controller-manager              v1.17.17   0ddd96ecb9e5   7 months ago    161MB
  k8s.gcr.io/kube-scheduler                       v1.17.17   d415ebbf09db   7 months ago    94.4MB
  k8s.gcr.io/kube-apiserver                       v1.17.17   38db32e0f351   7 months ago    171MB
  k8s.gcr.io/coredns                              1.6.5      70f311871ae1   21 months ago   41.6MB
  k8s.gcr.io/etcd                                 3.4.3-0    303ce5db0e90   22 months ago   288MB
  k8s.gcr.io/pause                                3.1        da86e6ba6ca1   3 years ago     742kB
  ---------
  # 前面的k8s.gcr.io就是我们打标签的结果，目的是让k8s可以识别到。因为官方下载的话，就是这个样子
  ```

  > ==**上面这些步骤需要在所有的集群节点中操作**==

  下面就是最后的集群初始化，只需要在我们的主节点上操作即可，

  ```bash
  # 使用kubeadm
  kubeadm init \
  --kubernetes-version=版本号，这里我们是 v1.17.17 \
  --pod-network-cidr=10.244.0.0/16 \
  --service-cidr=10.96.0.0/12 \
  --apiserver-advertise-address=主节点的ip
  # 再创建几个必要文件
  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config
  # 实际上面几句命令，是在init命令成功后，自动显示处理的提示
  ```

  此时，基本是完成集群操作，但是其它的从节点还没有设置，

  ```basic
  # 在前面的init运行后，最后还有一串命令，形似
  # kubeadm join ip:6443 --token xxx \ --discovery-token-ca-cert-hash sha256:xxx
  # 复制，放到其它的从节点上运行即可与当前的主节点建立称为集群
  # 在主节点运行一下命令，就可以看到加入了新节点
  kubectl get nodes
  ```

  但此时，节点直接的通信还是存在问题的，需要在主节点额外安装一个网络插件，

  ```basic
  # wget https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
  # 很不幸，又是国情，估计是下不了，读者可以自己想办法，或到
  # https://github.com/flannel-io/flannel/blob/master/Documentation/kube-flannel.yml
  # 但作为国内的环境，即使下载了，也需要该其中的仓库地址，不如直接使用阿里云的
  # https://github.com/flannel-io/flannel/blob/master/Documentation/kube-flannel-aliyun.yml
  # 下载到对应的文件中，随便放在linux的什么位置，运行它
  kubectl apply -f kube-flannel-aliyun.yml
  # 运行成功即可
  ```

# 使用

我们这里先用nginx测试一下。

```bash
# 部署nginx
kubestl create deployment nginx --image=nginx:1.14-alpine
# 暴露端口
kubectl expose deployment nginx --port=80 --type=NodePort
# 查看服务状态
kubectl get pod
kubectl get service
```

# 资源管理

就像java中一切皆是对象那样，在k8s中，一切皆是资源，对k8s的操作就是对资源的操作。

> 我们知道k8s是用来管理容器的，但是，实际情况确实，k8s并不会直接去接触容器。
>
> 我们部署的注入nginx都属于一个容器，它是放在一个`	Pod`中，这是k8s的最小管理单位。
>
> 但是，k8s也不常直接处理Pod，而是使用`Pod控制器`，来做管理。
>
> 当Pod准备提供服务时，k8s则使用`Service`资源帮助k8s访问Pod的服务。
>
> ```mermaid
> flowchart TD
> 
> subgraph Pod
> c1[Container];c2[Container]
> end
> style c1 fill:#7ceed4,stroke:#333
> style c2 fill:#7ceed4,stroke:#333
> subgraph PodController
> 	d[DaemonSet];r[ReplicaSet];Job;s[StatefulSet];Deployment --- r;ConJob---Job
> 	style d fill:#9ff63b
> 	style r fill:#9ff63b
> 	style Job fill:#9ff63b
> 	style s fill:#9ff63b
> 	style Deployment fill:#9ff63b
> 	style ConJob fill:#9ff63b
> end
> d---Pod;r---Pod;Job---Pod;s---Pod
> o---v[(Volume__负责资源数据存储)]
> Pod---S[Service__负责让外部与容器建立联系]
> v---ConfigMap
> v---PVC
> v---Secret
> style ConfigMap fill:#fcef54
> style PVC fill:#fcef54
> style Secret fill:#fcef54
> ```

## 管理方式

> 还是主要拿nginx做示范

- 命令式对象管理

  ```basic
  kubectl run nginx-pod --image=nginx:版本号 --port=80
  # 直接操作对应的资源
  ```

- 命令式对象配置

  ```basic
  kubectl create或patch -f nginx-pod.yaml
  # 使用配置文件操作资源
  # create自然代表创建，而patch则对应的是更新
  ```

- 声明式对象配置

  ```basic
  kubectl apply -f nginx-pod.yaml
  # 换了各方式利用配置文件做操作
  # 该方式，只能做创建和更新操作
  # 其中一个优势是，可以对目录做操作
  # 我们可以把所需要使用的yaml文件放在一个目录下
  # 声明式的操作可以一次性利用目录下的所有yaml文件做操作
  ```

### 命令式对象管理

- 语法

  ```basic
  kubectl [command] [type] [name] [flags]
  # 详细的细节可以 kubectl --help 查看一下
  # command: 可行的操作，如create、edit、get、patch、delete、explain
  # type: 资源类型，如deployment、pod、service
      # type的支持的类型，可以使用 kubectl api-resources 查看
  # name: 资源名称
  # flags: 额外的可选参数
      # 比如可以让展示的结果更详细，可以加个 -o wide，例如
      kubectl get pod pod_name -o wide
      # 又或者，让展示的结果显示为jsoN格式，并且信息可能更丰富。其它，还可以yaml格式
      kubectl get pod pod_name -o json
  # -----------
  # 例如
  # 查看所有pod
  kubectl get pod
  # 查看某个pod
  kubectl get pod pod_name
  # 查看某个pod，以yaml格式展示
  kubectl get pod pod_name -o yaml
  ```

  > 摘自kubectl  --help的结果
  >
  > > Basic Commands (Beginner):
  > >
  > > | create | Create a resource from a file or from stdin.                 |
  > > | ------ | ------------------------------------------------------------ |
  > > | expose | Take a replication controller, service, deployment or pod and expose it as a new Kubernetes Service |
  > > | run    | Run a particular image on the cluster                        |
  > > | set    | Set specific features on objects                             |
  > >
  > > Basic Commands (Intermediate):
  > >
  > > | explain | 展示资源文档                                                 |
  > > | ------- | ------------------------------------------------------------ |
  > > | get     | Display one or many resources                                |
  > > | edit    | Edit a resource on the server                                |
  > > | delete  | Delete resources by filenames, stdin, resources and names, or by resources and label selector |
  > >
  > > Deploy Commands:
  > >
  > > | rollout   | 管理资源的发布                                               |
  > > | --------- | ------------------------------------------------------------ |
  > > | scale     | 为部署、ReplicaSet或复制控制器设置新的大小                   |
  > > | autoscale | Auto-scale a Deployment, ReplicaSet, or ReplicationController |
  > >
  > >   Cluster Management Commands:
  > >
  > > | certificate  | Modify certificate resources.                |
  > > | ------------ | -------------------------------------------- |
  > > | cluster-info | Display cluster info                         |
  > > | top          | Display Resource (CPU/Memory/Storage) usage. |
  > > | cordon       | Mark node as unschedulable                   |
  > > | uncordon     | Mark node as schedulable                     |
  > > | drain        | Drain node in preparation for maintenance    |
  > > | taint        | Update the taints on one or more nodes       |
  > >
  > >   Troubleshooting and Debugging Commands:
  > >
  > > | describe     | 用于查看资源内部的各种细节                         |
  > > | ------------ | -------------------------------------------------- |
  > > | logs         | Print the logs for a container in a pod            |
  > > | attach       | 进入一个正在运行的容器                             |
  > > | exec         | Execute a command in a container                   |
  > > | port-forward | Forward one or more local ports to a pod           |
  > > | proxy        | Run a proxy to the Kubernetes API server           |
  > > | cp           | Copy files and directories to and from containers. |
  > > | auth         | Inspect authorization                              |
  > >
  > >  Advanced Commands:
  > >
  > > | diff      | Diff live version against would-be applied version           |
  > > | --------- | ------------------------------------------------------------ |
  > > | apply     | Apply a configuration to a resource by filename or stdin     |
  > > | patch     | Update field(s) of a resource using strategic merge patch    |
  > > | replace   | Replace a resource by filename or stdin                      |
  > > | wait      | Experimental: Wait for a specific condition on one or many resources. |
  > > | convert   | Convert config files between different API versions          |
  > > | kustomize | Build a kustomization target from a directory or a remote url. |
  > >
  > >   Settings Commands:
  > >
  > > | label      | Update the labels on a resource                              |
  > > | ---------- | ------------------------------------------------------------ |
  > > | annotate   | Update the annotations on a resource                         |
  > > | completion | Output shell completion code for the specified shell (bash or zsh) |
  > >
  > > Other Commands:
  > >
  > > | api-resources | Print the supported API resources on the server              |
  > > | ------------- | ------------------------------------------------------------ |
  > > | api-versions  | Print the supported API versions on the server, in the form of "group/version" |
  > > | config        | Modify kubeconfig files                                      |
  > > | plugin        | Provides utilities for interacting with plugins.             |
  > > | version       | Print the client and server version information              |
  >
  > > type的几种类型，【不全】
  > >
  > > 集群级别：
  > >
  > > | nodes      | 集群组成部分 | no   |
  > > | ---------- | ------------ | ---- |
  > > | namespaces | 隔离Pod      | ns   |
  > >
  > > > ```bash
  > > > # 例如，创建一个dev的命名空间
  > > > kubectl create namespaces dev
  > > > # 查看一下当前存在的命名空间
  > > > kubectl get ns
  > > > # 在dev中创建一个运行nginx的Pod
  > > > kubectl  run pod --image=nginx:版本号 -n dev
  > > > # 查看这个pod
  > > > kubectl get pod -n dev
  > > > # 结果会返回pod的名字，准备情况，状态和寿命等信息
  > > > # 我们可以看一下这个pod的内部细节
  > > > kubectl describe pods pod名字 -n dev
  > > > # 删除这个pod
  > > > kubectl delete pods pod名字 -n dev
  > > > # 上面的过程，最大的注意点在于，如果是操作特定命名空间的资源，一定要注意指明对应的命名空间
  > > > # 但是，不同读者想的的那样，我们删除了pod，但再去查看dev中时，还会有一个新的pod
  > > > # 这是因为pod的控制器会去自动补充缺失的资源
  > > > # 删除命名空间 dev
  > > > kubectl delete ns dev
  > > > # 者就会导致内部所有的资源都删除
  > > > ```
  > > >
  > > > 
  > >
  > > Pod资源：
  > >
  > > | pods | 装载容器 | po   |
  > > | ---- | -------- | ---- |
  > >
  > > pod资源控制器
  > >
  > > | replicationcontrollers   |      | rc     |
  > > | ------------------------ | ---- | ------ |
  > > | replicasets              |      | rs     |
  > > | deployments              |      | deploy |
  > > | daemonsets               |      | ds     |
  > > | jobs                     |      |        |
  > > | cronjobs                 |      | cj     |
  > > | horizontalpodautoscalers |      | hpa    |
  > > | statefulsets             |      | sts    |
  > >
  > > 服务发现资源：
  > >
  > > | services | 统一pod对外接口 | svc  |
  > > | -------- | --------------- | ---- |
  > > | ingress  | 统一pod对外接口 | ing  |
  > >
  > > 存储资源：
  > >
  > > | volumeattachments | 存储 |      |
  > > | ----------------- | ---- | ---- |
  > > | persistentvolumes |      | pv   |
  > >
  > > 配置资源：
  > >
  > > | configmaps |      | cm   |
  > > | ---------- | ---- | ---- |
  > > | secrets    |      |      |
  > >
  > > 







