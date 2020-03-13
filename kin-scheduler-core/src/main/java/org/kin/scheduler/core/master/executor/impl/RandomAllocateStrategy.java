package org.kin.scheduler.core.master.executor.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.master.ExecutorRes;
import org.kin.scheduler.core.master.WorkerContext;
import org.kin.scheduler.core.master.WorkerRes;
import org.kin.scheduler.core.master.domain.SubmitJobRequest;
import org.kin.scheduler.core.master.executor.AllocateStrategy;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RandomAllocateStrategy implements AllocateStrategy {

    @Override
    public List<WorkerRes> allocate(SubmitJobRequest request, Collection<WorkerContext> workerContexts, Collection<ExecutorRes> usedExecutorReses) {
        if (CollectionUtils.isNonEmpty(workerContexts) && CollectionUtils.isEmpty(usedExecutorReses)) {
            List<WorkerContext> workerContextList = new ArrayList<>(workerContexts);

            Random random = new Random();
            return Collections.singletonList(new WorkerRes(workerContextList.get(random.nextInt(workerContextList.size())).getWorkerInfo().getWorkerId()));
        }

        return null;
    }
}
