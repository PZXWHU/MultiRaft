运行每次测试时，应删除项目目录下生成的raftHome目录，避免造成影响。

## 测试选举
1. 依次允许运行RaftNodeElection1、RaftNodeElection2、RaftNodeElection3，选举生成leader
2. 关闭leader进程，剩余两个follower生成新的leader
3. 重启关闭的leader进程，重新加入集群变成follower

## 测试日志复制
1. 依次允许运行RaftNodeElection1、RaftNodeElection2、RaftNodeElection3，选举生成leader
2. 运行RaftNodeReplicateLog1，客户端写入数据，leader进行日志复制，当复制到大部分节点后，返回客户端。
3. 关闭leader进程，运行RaftNodeReplicateLog2，客户端读取刚才写入的数据（注意：此时客户端不能连接刚才关闭的节点）。

## 测试成员变更
1. 依次允许运行RaftNodeElection1、RaftNodeElection2、RaftNodeElection3，选举生成leader
2. 运行RaftNodeClusterChange1, 客户端写入增加节点配置，leader进行日志复制，当复制到大部分节点后，返回客户端。
3. 运行RaftNodeClusterChange2，运行新节点，新节点加入集群，并接收leader的复制日志。
4. 运行RaftNodeClusterChange3, 删除已存在的节点。

## 测试快照
1. 依次允许运行RaftNodeElection1、RaftNodeElection2、RaftNodeElection3，选举生成leader
2. 运行RaftNodeReplicateLog1，客户端写入数据，leader进行日志复制，当复制到大部分节点后，返回客户端。
3. 关闭RaftNodeElection1、启动RaftNodeSnapshot1，生成快照。
4. 关闭RaftNodeSnapshot1，启动RaftNodeElection1，节点重启，加载快照恢复状态。（验证节点重启加载快照功能）
5. 关闭RaftNodeElection2、RaftNodeElection3，删除raftHome2和raftHome2主目录
6. 重启RaftNodeElection2、RaftNodeElection3， 保证RaftNodeElection1为leader， 然后leader会将快照复制给follower
7. 运行RaftNodeReplicateLog2，客户端读取数据
