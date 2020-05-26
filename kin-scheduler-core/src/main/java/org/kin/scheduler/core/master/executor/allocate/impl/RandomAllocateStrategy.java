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
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RandomAllocateStrategy implements AllocateStrategy {

    @Override
    public List<WorkerResource> allocate(Collection<WorkerContext> workerContexts, Collection<ExecutorResource> usedExecutorRese) {
        if (CollectionUtils.isNonEmpty(workerContexts) && CollectionUtils.isEmpty(usedExecutorRese)) {
            List<WorkerContext> workerContextList = new ArrayList<>(workerContexts);

            //TODO 目前选择占用全部CPU
            WorkerContext workerContext = workerContextList.get(ThreadLocalRandom.current().nextInt(workerContextList.size()));
            return Collections.singletonList(
                    new WorkerResource(workerContext.getWorkerInfo().getWorkerId(), workerContext.getWorkerInfo().getMaxCpuCore()));
        }

        return null;
    }
}
