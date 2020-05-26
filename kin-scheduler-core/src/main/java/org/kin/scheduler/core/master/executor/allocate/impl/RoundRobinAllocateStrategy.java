package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.master.domain.ExecutorResource;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RoundRobinAllocateStrategy implements AllocateStrategy {
    private AtomicInteger round = new AtomicInteger(0);

    @Override
    public List<WorkerResource> allocate(Collection<WorkerContext> workerContexts, Collection<ExecutorResource> usedExecutorRese) {
        if (CollectionUtils.isNonEmpty(workerContexts) && CollectionUtils.isEmpty(usedExecutorRese)) {
            List<WorkerContext> workerContextList = new ArrayList<>(workerContexts);
            WorkerContext selected = workerContextList.get(next(workerContextList.size()));
            //TODO 目前选择占用全部CPU
            return Collections.singletonList(new WorkerResource(selected.getWorkerInfo().getWorkerId(), selected.getWorkerInfo().getMaxCpuCore()));
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
