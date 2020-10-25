package com.pzx.rpc.service.balancer;

import java.util.List;

public interface LoadBalancer {
    <T> T select(List<T> list);
}
