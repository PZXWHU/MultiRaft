# Raft

Raft 通过选举一个高贵的领导人，然后给予他全部的管理复制日志的责任来实现一致性。领导人从客户端接收日志条目，把日志条目复制到其他服务器上，并且当保证安全性的时候告诉其他的服务器应用日志条目到他们的状态机中。

通过领导人的方式，Raft 将一致性问题分解成了三个相对独立的子问题，这些问题会在接下来的子章节中进行讨论：

- **领导选举**：一个新的领导人需要被选举出来，当现存的领导人宕机的时候（章节 5.2）
- **日志复制**：领导人必须从客户端接收日志然后复制到集群中的其他节点，并且强制要求其他节点的日志保持和自己相同。
- **安全性**：在 Raft 中安全性的关键是在图 3 中展示的状态机安全：如果有任何的服务器节点已经应用了一个确定的日志条目到它的状态机中，那么其他服务器节点不能在同一个日志索引位置应用一个不同的指令。

## 领导选举
### Timeout
1. In Raft there are two timeout settings which control elections.
2. election timeout：The election timeout is the amount of time a follower waits until becoming a candidate.（一般150ms - 300ms），After the election timeout the follower becomes a candidate and starts a new election term
3. heartbeat timeout：The leader begins sending out Append Entries messages to its followers.These messages are sent in intervals specified by the heartbeat timeout.The follower reset election timeout when receive heartbeat
4. election timeout > heartbeat timeout

### 启动之后
1. 当服务器程序启动时，他们都是跟随者身份
2. 领导者周期性的向所有跟随者发送心跳包，一个服务器节点继续保持着跟随者状态只要他从领导人或者候选者处接收到有效的 RPCs
3. 如果一个跟随者在一段时间里没有接收到任何消息，也就是**选举超时**（ election timeout），则发起选举以选出新的领导者

### 进行领导选举
1. 跟随者先要增加自己的当前任期号并且转换到候选人状态
2. 然后他会投票给自己，然后并行的向集群中的其他服务器节点发送请求投票的 RPCs 来给自己投票
3. 候选人会继续保持着当前状态直到以下三件事情之一发生：(a) 他自己赢得了这次的选举；(b) 其他的服务器成为领导者；(c) 一段时间之后没有任何一个获胜的人

### 领导选举结果
1. 当一个候选人从整个集群的大多数服务器节点获得了针对同一个任期号的选票，那么他就赢得了这次选举并成为领导人。然后他会向其他的服务器发送心跳消息来建立自己的权威并且阻止新的领导人的产生。
2. 在等待投票的时候，候选人可能会从其他的服务器接收到声明它是领导人的附加日志项 RPC。如果这个领导人的任期号（包含在此次的 RPC中）不小于候选人当前的任期号，那么候选人会承认领导人合法并回到跟随者状态。 如果此次 RPC 中的任期号比自己小，那么候选人就会拒绝这次的 RPC 并且继续保持候选人状态。
3. 如果有多个跟随者同时成为候选人，那么选票可能会被瓜分以至于没有候选人可以赢得大多数人的支持。当这种情况发生的时候，每一个候选人都会超时（ election timeout），然后通过增加当前任期号来开始一轮新的选举。

### 领导选举限制
1. 一个节点在一次任期内只能投一次票
2. 请求投票RPC 中包含了候选人的日志信息，然后投票人会拒绝掉那些日志没有自己新的投票请求。