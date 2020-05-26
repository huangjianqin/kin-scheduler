package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.HashUtils;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.master.domain.ExecutorResource;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class HashAllocateStrategy implements AllocateStrategy {
    private static final int LIMIT = 9;

    @Override
    public List<WorkerResource> allocate(Collection<WorkerContext> workerContexts, Collection<ExecutorResource> usedExecutorRese) {
        if (CollectionUtils.isNonEmpty(workerContexts) && CollectionUtils.isEmpty(usedExecutorRese)) {
            TreeMap<Integer, WorkerContext> map = new TreeMap<>();
            for (WorkerContext workerContext : workerContexts) {
                map.put(HashUtils.efficientHash(workerContext, LIMIT), workerContext);
            }

            WorkerContext selected = map.firstEntry().getValue();
            if (Objects.nonNull(selected)) {
                //TODO 目前选择占用全部CPU
                return Collections.singletonList(
                        new WorkerResource(selected.getWorkerInfo().getWorkerId(), selected.getWorkerInfo().getMaxCpuCore()));
            }
        }

        return null;
    }
}
