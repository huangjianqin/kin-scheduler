package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.List;

/**
 * 分配全部资源
 *
 * @author huangjianqin
 * @date 2020-03-10
 */
public class AllAllocateStrategy implements AllocateStrategy {

    @Override
    public List<WorkerContext> allocate(List<WorkerContext> workerContexts) {
        return workerContexts;
    }
}
