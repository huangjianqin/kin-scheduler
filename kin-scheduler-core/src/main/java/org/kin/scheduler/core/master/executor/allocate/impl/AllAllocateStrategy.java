package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.master.ExecutorRes;
import org.kin.scheduler.core.master.WorkerContext;
import org.kin.scheduler.core.master.WorkerRes;
import org.kin.scheduler.core.master.domain.SubmitJobRequest;
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
    public List<WorkerRes> allocate(SubmitJobRequest request, Collection<WorkerContext> workerContexts, Collection<ExecutorRes> usedExecutorReses) {
        if (CollectionUtils.isNonEmpty(workerContexts) && CollectionUtils.isEmpty(usedExecutorReses)) {
            return workerContexts.stream().map(wc -> new WorkerRes(wc.getWorkerInfo().getWorkerId())).collect(Collectors.toList());
        }
        return null;
    }
}
