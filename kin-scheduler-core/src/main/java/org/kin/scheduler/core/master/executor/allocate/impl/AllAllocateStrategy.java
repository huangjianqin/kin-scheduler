package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.master.domain.ExecutorResource;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-03-10
 */
public class AllAllocateStrategy implements AllocateStrategy {

    @Override
    public List<WorkerResource> allocate(Collection<WorkerContext> workerContexts, Collection<ExecutorResource> usedExecutorRese) {
        if (CollectionUtils.isNonEmpty(workerContexts) && CollectionUtils.isEmpty(usedExecutorRese)) {
            //TODO 目前选择占用全部CPU
            return workerContexts.stream()
                    .map(wc -> new WorkerResource(wc.getWorkerInfo().getWorkerId(), wc.getWorkerInfo().getMaxCpuCore()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}
