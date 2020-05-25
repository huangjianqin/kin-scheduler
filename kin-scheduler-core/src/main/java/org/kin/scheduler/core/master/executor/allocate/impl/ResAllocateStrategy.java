package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.scheduler.core.domain.WorkerRes;
import org.kin.scheduler.core.master.domain.ExecutorRes;
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
    public List<WorkerRes> allocate(Collection<WorkerContext> workerContexts, Collection<ExecutorRes> usedExecutorReses) {
        //TODO
        return null;
    }
}
