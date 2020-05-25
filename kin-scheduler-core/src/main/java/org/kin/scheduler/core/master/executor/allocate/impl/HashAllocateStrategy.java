package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.HashUtils;
import org.kin.scheduler.core.domain.WorkerRes;
import org.kin.scheduler.core.master.domain.ExecutorRes;
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
    public List<WorkerRes> allocate(Collection<WorkerContext> workerContexts, Collection<ExecutorRes> usedExecutorReses) {
        if (CollectionUtils.isNonEmpty(workerContexts) && CollectionUtils.isEmpty(usedExecutorReses)) {
            TreeMap<Integer, WorkerContext> map = new TreeMap<>();
            for (WorkerContext workerContext : workerContexts) {
                map.put(HashUtils.efficientHash(workerContext, LIMIT), workerContext);
            }

            WorkerContext selected = map.firstEntry().getValue();
            if (Objects.nonNull(selected)) {
                return Collections.singletonList(new WorkerRes(selected.getWorkerInfo().getWorkerId()));
            }
        }

        return null;
    }
}
