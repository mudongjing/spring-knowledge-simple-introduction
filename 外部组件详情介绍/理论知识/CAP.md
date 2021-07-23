本文是介绍分布式比较关键的一致性问题。学术的说是

> State Machine Replication Consensus
>
> 状态机复制的共识

CAP：一致性（Consistency）、可用性（Availability）、分区容错性（Partition Tolerance）。

# 模型

- 弱一致性：
  - 最终一致性：不保证，其它节点的数据能立刻同步
    - DNS（Domain Name System)
    - Gossip：Cassandra通信协议
- 强一致性
  - 同步
  - Paxos
  - Raft（multi-paxos)
  - ZAB（multi-paxos)

# Paxos

算法角色，

> Client:系统外部角色，请求的发起者
>
> Proposer:接受Client请求，像集群发出提议（propose）。当冲突时，起调节作用。
>
> Accpetor(Voter)：提议投票者和接收者，只要赞成大多数（Quorum)，提议就接受。
>
> Learner:提议接受者，backup。备份。相当于记录员，对集群一致性没有影响。

- Basic Paxos

  1. 阶段1，准备

     proposer提出议案，并标号N,这个proposer之前提出的议案的编号都是要小于这个N的。

  2. 阶段2，许诺

     对于一个accpetor,如果N大于它之前接受的任意议案的编号，它就同意，否则反对

  3. 阶段3，去接受

     当同意的人数较多，即称为多数派，proposer就正式像所有acceptor发出accept请求，内容包含了编号和内容

  4. 阶段4，接受了

     在这期间，对于一个acceptor而言，如果没有再收到大于N的议案，就接受这个议案，否则就忽略。

     > 这期间，很多任务都是proposer负责，它既需要发送议案，有需要监听acceptor的同意情况，以确定是否满足接受要求。在最后，也需要proposer负责发最后的请求接受发给每个acceptor，并监听是否每个都返回了确认信号，以最后保证，所有的acceptor都接受这个议案。
     >
     > 当proposer中间宕机了，那么则需要一个新的proposer接替工作，并将原有的议案作为一个新议案再重复流程。编号自然更大。

  最后，全部接受后，再由Learner记录并备份，当前的改变，再把情况发送给那个Client。

  该机制的问题，活锁

  > 当两个用户试图通过自己的议案，由于编号大的占优势，那么当一方被编号大的一方挤下线，议案根本还没有审核，那么它会将自己的议案编号增大，再次申请，就会把之前的那个议案也挤下线，恶性循环，导致，两个议案一直在争夺谁的编号更大，导致变相地阻塞进程。
  >
  > - 解决方法，就是让双方谦让一些，当就进程发现有一个新的议案试图申请，且对方的编号更大，自己就先退却个一秒钟，等待新议案结束，旧议案将会以更大的编号再次申请，此时，若对方面临旧议案当初的局面，自然也会做出同样的决定。

  ~~这一机制，仅作了解，没人会真的去实现它。~~

- Multi Paxos

  新角色，Leader

  > 唯一的proposer,所有的请求都有它负责

  不同之处在于，这个模式下，需要proposer们先自己选举出一个Leader，之后，进入的请求就有这个leader处理，活锁的问题的也就不存在了。

但终究，Paxos的不好实现，就需要更简单的协议

# Raft

这个算法是对上述Paxos的Multi Paxos的简化。

角色，

> Leader
>
> Follower
>
> Candidate:他是leader的候选者，leader不宕机，就没它的事

试图处理的问题

> Leader Election
>
> Log Replication：就是当前leader的日志需要同步到它的Candidate中
>
> Safety：如何保证在操作期间，集群中的节点，共识都是一致的，规避错误的出现

- Leader Election

  这个问题很好解决，只需要一个节点发起请求，试图称为leader,并询问其它节点是否同意

- Log Replication

  当leader接受了新的请求，既出现了一个新的议案，leader需要把这个情况和其它candidate同步一下，首先发送这个情况给candidate们，当candidate接收到信息则发送信息回去。

  当大部分的candidate已经收到消息，那么leader就认为差不多了，自己把这个新议案真正写入自己的日志中，在告诉其它节点也把消息写到自己的日志中。

- Safety

  这实际是保证集群中一直有leader的存在，这里就是涉及心跳机制，leader定期发送心跳信息给candidate，如果candidate发现这次的心跳迟到了，那么就认为leader宕机，就自动地尝试竞选leader。

  当新的leader产生了，心跳机制自然也会刷新一遍。

> 心跳信息，其实也附带着leader试图发送的一些数据

# ZAB

它于Raft基本相似。

不同点在于，

> 心跳方向，Raft是leader负责发送，而这里是其它节点发送给leader,相当于主动检测leader的有效性。