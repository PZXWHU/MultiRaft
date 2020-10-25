package com.pzx.rpc.service.balancer;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomBalancer implements LoadBalancer {

    private RandomBalancer(){}

    @Override
    public <T> T select(List<T> list) {
        Random random  = ThreadLocalRandom.current();
        return list.get(random.nextInt(list.size()));
    }
}
