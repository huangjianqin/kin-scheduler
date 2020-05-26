package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.master.domain.ExecutorResource;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.Collection;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class ResAllocateStrategy implements AllocateStrategy {

    @Override
    public List<WorkerResource> allocate(Collection<WorkerContext> workerContexts, Collection<ExecutorResource> usedExecutorRese) {
        //TODO
        return null;
    }
}
