package org.kin.scheduler.core.master.executor.impl;

import org.kin.framework.utils.CollectionUtils;
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
    public List<WorkerRes> allocate(SubmitJobRequest request, Collection<WorkerContext> workerContexts) {
        if (CollectionUtils.isNonEmpty(workerContexts)) {
            List<WorkerContext> workerContextList = new ArrayList<>(workerContexts);
            if (workerContextList.size() == 1) {
                return Collections.singletonList(new WorkerRes(workerContextList.get(0).getWorkerInfo().getWorkerId()));
            }

            Random random = new Random();
            return Collections.singletonList(new WorkerRes(workerContextList.get(random.nextInt(workerContextList.size())).getWorkerInfo().getWorkerId()));
        }

        return null;
    }
}
