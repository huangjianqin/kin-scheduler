package org.kin.scheduler.core.master.executor.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.master.ExecutorRes;
import org.kin.scheduler.core.master.WorkerContext;
import org.kin.scheduler.core.master.WorkerRes;
import org.kin.scheduler.core.master.domain.SubmitJobRequest;
import org.kin.scheduler.core.master.executor.AllocateStrategy;

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
    public List<WorkerRes> allocate(SubmitJobRequest request, Collection<WorkerContext> workerContexts, Collection<ExecutorRes> usedExecutorReses) {
        if (CollectionUtils.isNonEmpty(workerContexts) && CollectionUtils.isEmpty(usedExecutorReses)) {
            List<WorkerContext> workerContextList = new ArrayList<>(workerContexts);
            WorkerContext selected = workerContextList.get(next(workerContextList.size()));
            return Collections.singletonList(new WorkerRes(selected.getWorkerInfo().getWorkerId()));
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
