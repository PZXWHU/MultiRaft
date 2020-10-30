# 项目概述
本项目是Raft分布式一致性协议的JAVA简易实现。RPC框架使用的是根据自己所写的简易RPC框架[MyRPC](https://github.com/PZXWHU/MyRPC)的改进版。

## 项目实现功能
1. leader选举
2. 日志复制
3. 集群成员变更
4. 快照功能

## 功能测试

[raft test](./raft-test/README.md)



## raft论文解读

[raft paper](./doc/raft.pdf)

[raft paper read](./doc/raft-paper-read.md)

## 项目参考
https://juejin.im/post/6844903759982624781
https://github.com/stateIs0/lu-raft-kv
https://github.com/maemual/raft-zh_cn/blob/master/raft-zh_cn.md
https://github.com/wenweihu86/raft-java


# 项目使用

## Raft节点配置文件

```java


{
  "nodeId" : 1, //节点ID，不能是0
  "raftHome" : "./raftHome1",//节点主目录，日志、快照储存位置
  "clusterAddress" : {//集群中所有节点的ID和地址
      "1" : "127.0.0.1:9999",
      "2" : "127.0.0.1:8999",
      "3" : "127.0.0.1:7999"
  }
    /*
   "int electionTimeoutMilliseconds" : 3000,

    "heartbeatPeriodMilliseconds" : 400,

    "snapshotPeriodSeconds" : 3600,

    "snapshotMinLogSize" : 100 * 1024 * 1024,

    "maxLogEntriesPerRequest: : 50,

    "maxAwaitTimeout: : 2000,

    "raftConsensusThreadNum: : 20,

    "asyncWrite" : false
     */

}

```



## Raft节点启动

```java
String configPath = "config.json";
RaftNode raftNode = new RaftNode(configPath);
raftNode.start();
```

## Raft客户端使用

### KVDatabase数据库操作

#### 创建服务代理

 ```java
ProxyConfig proxyConfig = new ProxyConfig().setDirectServerUrl("127.0.0.1:7999").setInvokeType(InvokeType.SYNC);//setDirectServerUrl设置集群中任意节点地址
RaftKVClientService raftKVClientService = proxyConfig.getProxy(RaftKVClientService.class);
 ```

#### 设置数据

```java

ClientKVResponse clientKVResponse = raftKVClientService.operateKV(ClientKVRequest.builder().type(ClientKVRequest.PUT).key("pzx").value("sx").build());
System.out.println(clientKVResponse);
```

#### 获取数据

```java
ClientKVResponse clientKVResponse = raftKVClientService.operateKV(ClientKVRequest.builder().type(ClientKVRequest.GET).key("pzx").build());
System.out.println(clientKVResponse);
```

### 集群节点变更

#### 创建服务代理

```java
ProxyConfig proxyConfig = new ProxyConfig().setDirectServerUrl("127.0.0.1:9999").setInvokeType(InvokeType.SYNC);
RaftClusterMembershipChangeService raftClusterMembershipChangeService = proxyConfig.getProxy(RaftClusterMembershipChangeService.class);
```

#### 添加节点

```java
ClusterMembershipChangeRequest request = ClusterMembershipChangeRequest.builder().nodeId(4).nodeAddress("127.0.0.1:6999").build();
ClusterMembershipChangeResponse response = raftClusterMembershipChangeService.addNode(request);
System.out.println(response);
```

#### 删除节点

```java
ClusterMembershipChangeRequest request = ClusterMembershipChangeRequest.builder().nodeId(1).nodeAddress("127.0.0.1:9999").build();
ClusterMembershipChangeResponse response = raftClusterMembershipChangeService.removeNode(request);
System.out.println(response);
```

