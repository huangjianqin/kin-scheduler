package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.domain.WorkerRes;
import org.kin.scheduler.core.master.domain.ExecutorRes;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;
import org.kin.scheduler.core.master.transport.SubmitJobRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RandomAllocateStrategy implements AllocateStrategy {

    @Override
    public List<WorkerRes> allocate(SubmitJobRequest request, Collection<WorkerContext> workerContexts, Collection<ExecutorRes> usedExecutorReses) {
        if (CollectionUtils.isNonEmpty(workerContexts) && CollectionUtils.isEmpty(usedExecutorReses)) {
            List<WorkerContext> workerContextList = new ArrayList<>(workerContexts);

            return Collections.singletonList(new WorkerRes(workerContextList.get(ThreadLocalRandom.current().nextInt(workerContextList.size())).getWorkerInfo().getWorkerId()));
        }

        return null;
    }
}
