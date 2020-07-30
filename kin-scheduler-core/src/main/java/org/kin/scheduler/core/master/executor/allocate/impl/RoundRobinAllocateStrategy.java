package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于RoundRobin(逐渐递增)的资源分配策略
 *
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RoundRobinAllocateStrategy implements AllocateStrategy {
    private AtomicInteger round = new AtomicInteger(0);

    @Override
    public List<WorkerContext> allocate(List<WorkerContext> workers) {
        if (CollectionUtils.isNonEmpty(workers)) {
            return Collections.singletonList(workers.get(next(workers.size())));
        }

        return null;
    }

    private int next(int size) {
        if (round.get() <= 0) {
            round.set(0);
        }
        return (round.getAndAdd(1) + size) % size;
    }
}
