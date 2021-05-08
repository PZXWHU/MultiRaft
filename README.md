## 项目概述

本项目使用java语言基于[MyRpc](https://github.com/PZXWHU/MyRPC)和[MyRaft](https://github.com/PZXWHU/MyRaft)项目之上实现MultiRaft。本项目是个人对Raft、MultiRaft的理解与实践尝试，无法保证高效率和bug-free。

**什么是MulitRaft ？**

引用Cockroach对Multi-Raft的定义

In [CockroachDB](https://github.com/cockroachdb/cockroach), we use the [Raft consensus algorithm](https://raftconsensus.github.io/) to ensure that your data remains consistent even when machines fail. In most systems that use Raft, such as [etcd](https://github.com/coreos/etcd) and [Consul](https://www.consul.io/), the entire system is one Raft consensus group. In CockroachDB, however, the data is divided into *ranges*, each with its own consensus group. This means that each node may be participating in hundreds of thousands of consensus groups. This presents some unique challenges, which we have addressed by introducing a layer on top of Raft that we call [MultiRaft](https://github.com/cockroachdb/cockroach/blob/8187c2551352a6c28eba021effaebcbfe523d78c/docs/RFCS/20151213_dismantle_multiraft.md).

## 核心功能实现

### Raft数据存储

### 元数据存储

### 心跳设计

### region分裂

### region查找

### 领导者转移

### 副本增加

### 网络通讯合并