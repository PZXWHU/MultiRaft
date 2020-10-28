## 快照与恢复
### 快照
#### 快照内容
1. 已经被提交的日志，即状态机的状态
2. 元数据：last included index（状态机最后应用的日志的索引）、last included term（状态机最后应用的日志的任期）、raft配置

#### 生成快照步骤
1. 考虑一些取消takeSnapshot的情况
2. takeSnapshot时可能在复制日志，应用日志等，所以要采用写时复制。加锁拷贝lastIncludedIndex, lastIncludedTerm，stateMachine，config;
3. 先将snapshot写入临时文件夹，避免take snapshot失败，覆盖了以前的snapshot
    - take metadata snapshot
    - take statemachine snapshot
4. 将tmpSnapshotDir覆盖snapshotDir
5. 如果take snapshot成功，则丢弃旧的日志条目

### 宕机恢复（先加载快照，后加载日志）
#### 加载快照
1. 读取元数据快照，恢复节点状态变量commitIndex、currentTerm、lastAppliedIndex、raftConfig
2. 状态机读取数据快照，恢复状态机状态

#### 加载日志
1. 加载节点持久化在日志中的元数据，恢复节点状态变量votedFor、commitIndex、currentTerm、lastAppliedIndex
2. 将快照中未包含的但是已经提交的日志条目应用到状态机