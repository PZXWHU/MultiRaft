## RPC全异步的思路
#### RPC客户端：

1. 创建CompletableFuture，将future对象记录在某处，比如RpcConetxt中，future应该被唯一标识，例如将RPC 的requestId作为key。返回future对象，作为RPC调用的结果。
2. 当客户端收到服务端的结果后，在RpcContext中找到对应的future对象，调用future的complete的方法，通知调用方Rpc的结果。
3. 调用方利用RPC调用返回的future对象可以设置回调函数等，当future的complete方法调用时，则激活回调函数。

#### RPC服务端：

1. 创建CompletableFuture，设置回调函数，当future对象的complete方法被调用时时，向RPC客户端返回Rpc的结果。
2. 上一步的future对象可以由RPC调用的服务产生，也可以由外部传给RPC调用的服务。中心思想时异步执行RPC调用的服务，当服务调用完成后，调用future对象的complete的方法。


## RPC的线程管理
#### RPC总是超时的异常
使用netty实现的Rpc，一定要注意线程管理，不能在channel的IO线程中再调用需要占用当前channel IO线程的方法（比如PRC同步方法）
比如在RpcResponseInboundHandler中调用RpcInvokeContext.completeFuture(rpcResponse)，
则会在当前channel的IO线程（如果没有设置业务线程池的话）中调用RpcInvokeContext.completeFuture。
如果RpcInvokeContext.completeFuture方法中又再次调用了当前channel的IO的同步方法，则会造成死阻塞。

比如RpcInvokeContext.completeFuture中调用了CompletableFuture的complete方法，则会触发whenComplete同步回调方法，
如果在该同步回调方法中进行了同步RPC调用，且这个Rpc调用需要用到当前channel的IO线程，但是当前IO线程又被当前回调方法占用，
所以同步RPC需要等待IO线程被释放，但是同步Rpc不完成，当前IO线程无法释放，所以陷入死锁