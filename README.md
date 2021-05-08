## 项目概述

本项目使用java语言基于[MyRpc](https://github.com/PZXWHU/MyRPC)和[MyRaft](https://github.com/PZXWHU/MyRaft)项目之上实现MultiRaft。本项目是个人对Raft、MultiRaft的理解与实践尝试，无法保证高效率和bug-free。

**什么是MulitRaft ？**

引用Cockroach对Multi-Raft的定义

In [CockroachDB](https://github.com/cockroachdb/cockroach), we use the [Raft consensus algorithm](https://raftconsensus.github.io/) to ensure that your data remains consistent even when machines fail. In most systems that use Raft, such as [etcd](https://github.com/coreos/etcd) and [Consul](https://www.consul.io/), the entire system is one Raft consensus group. In CockroachDB, however, the data is divided into *ranges*, each with its own consensus group. This means that each node may be participating in hundreds of thousands of consensus groups. This presents some unique challenges, which we have addressed by introducing a layer on top of Raft that we call [MultiRaft](https://github.com/cockroachdb/cockroach/blob/8187c2551352a6c28eba021effaebcbfe523d78c/docs/RFCS/20151213_dismantle_multiraft.md).

## 核心功能实现

### MultiRaft数据存储

MultiRaft集群的每个Store节点上会有多个region，每个region都包含一个raftNode，每个raftNode

都拥有自己的状态机、日志存储、元数据。

本项目在设计上将Store节点上的所有raftNode的所有数据存储在一个RocksDB中。

- 不同的raftNode以其所在的regionId作为前缀进行区别。

- 不同类型的数据，设置相应前缀进行区别：状态机（“sm”），日志（"log"）,元数据（“meta）

将所有raftNode的数据存放在一个RocksDB实例中，减少了Store节点所需要维护的打开文件数量，只要顺序写入一个wal，提高了写入的效率。另外由于RocksDB的数据文件是有序的，所以数据查询读取的复杂度是对数级的，所以将所有region的数据存储在一起不会导致数据查询效率大幅下降。不同类型的数据是否存放同一个RocksDB实例中，可以进行选择。

### 元数据存储

MultiRaft集群的元数据全部存储在PlaceDriverServer端，Store节点不持久化存储Store和Region的元信息。这里的元数据不是指raftNode持久化存储的元数据。

Store元数据包括：store节点上包含哪些region等，Region元数据包括：Region的start/end Key，regionEpoch，raftGroupAddress等。Store和Region元数据通过心跳或者主动更新发送到Pd端进行持久化存储，Store节点启动时，访问Pd端获取元数据进行恢复。

### 心跳设计

PlaceDriverServer端不会主动发送RPC请求到Store节点，Pd和Store的所有通讯都通过心跳进行，这样设计减少了Pd实现的复杂性。

心跳分为Store heartbeat和Region heartbeat。

- Store heartbeat由Store节点定时发送，内容包括Store的元数据以及Store的统计信息（Store节点包含的region数目，region leader的数量等）
- Region heartbeat由Region leader定时发送，内容包括Region的元数据以及Region的统计信息（Region的存活副本数量，region当前包括kv的数量等）

Pd根据心跳中的元数据进行持久化更新，根据统计信息做出相应的指令（region分裂、领导者转移、副本增加/删除等）。

### region分裂

**1.首先考虑单个Store节点如果进行region分裂：**

- split key：对于一个region分裂，必须找到split key。本项目region size和region split key的获取都是靠db scan的方式获取的，这种方式效率比较低下。
- region epoch：region分裂/转移、副本增减都需要将region epoch自增。 

- 状态机：不需要处理状态机数据的分裂，因为所有region的数据都存储在一个RocksDB实例中，而且状态机数据是没有RegionId前缀的。
- 日志：不需要处理日志数据的分裂，即被分裂的Region仍然保留之前的所有的日志。因为对于非幂等性的状态机，已经applied的日志不会再次apply，所以分裂的oldRegion保留旧日志不会有问题，因为不会再次日志apply。对于幂等性的状态机，由于我们采用的是多个region的状态机公用一个底层存储，所以分裂的region保留旧日志不会有问题，因为多次apply日志无影响。
- 快照：生成的的new Region需要立刻进行snapshot。因为new Regoin中的数据日志在old Region log中，如果old Region snapshot之后，这部分数据日志会被删除，而old Region snapshot也不包含new region中的数据，则有可能导致数据丢失。

**2.其次考虑多个Store节点如何进行一致region分裂：**

- 所有Store节点上的region分裂可以看作是一个分布式共识问题，所以使用raft协议解决。
- 当region leader接收到Pd端发送的split指令后，在当前raft Node上写入split log，当日志被复制到大部分节点上后，则应用split log。
- 当应用split log时，修改的region的元数据（start/end key, epoch），创建新的reigon，并进行snapshot。

**3.然后考虑Region分裂时的读写一致性：**

- region分裂时的写入：所有客户端写入都需要携带region epoch，所有写入都要在日志propose和apply时进行epoch的检测。因为raft日志是有序apply的：
  - 如果数据写入时，raftNode还未apply 分裂日志，那么写入日志和分裂日志顺序写入被raft log中，当apply 写入日志时，分裂日志已经被apply，此时region epoch已经增加，所以apply写入日志时检测region epoch失败，写入日志被skip，写入失败。
  - 如果写入的节点已经应用分裂日志，那么则正常写入
- region分裂时的读取：在region分裂的时候，停止被分裂的region提供的读取操作。否则可能产生线性一致性的问题。参考：[Split 过程中的一致性](https://segmentfault.com/a/1190000023869223)。如何识别region正在分裂见下文。

**4.最后考虑如何处理pd产生多次重复分裂的指令：**

由于分裂的耗时性，我们必须要考虑pd产生的对一个region的多次重复的分裂日志，通过判断region是否正在分裂，决定是否进行分裂操作：

- 如果region的分裂日志已经被应用，那么重复的分裂指令中的regionEpoch一定会过期，则跳过此次分裂
- 如果region的分裂日志已经写入还未被应用，则region一定显示正在分裂中。**判断region是否正在分裂的方法：分裂日志在写入raftlog中时，在raft meta中记录下lastSplitLogIndex**。**所以lastSplitLogIndex < lastAppliedIndex，则分裂日志还未应用，region正处于分裂的过程中，则跳过此次分裂**。

### region查找

由于region会根据集群的负载进行转移、增加、销毁等，所以客户端在访问region中的数据时，需要先访问Pd端的路由表，查询数据所在region的信息，再向region leader发送读写请求。

Pd端的路由表可由跳表或者搜索树实现，本项目使用 ConcurrentSkipListMap实现。以Region的startKey作为key，regionId作为value，通过floorEntry以及subMap方法实现点查和范围查询。

**另外路由表必须和region元数据保持一致性，即region在更新的时候（比如分裂），应该同步更新路由表。**

### 领导者转移

pending

### 副本增加

pending

### 网络通讯合并

pending