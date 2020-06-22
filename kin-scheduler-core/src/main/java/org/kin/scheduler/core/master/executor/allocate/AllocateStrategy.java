package org.kin.scheduler.core.master.executor.allocate;

import org.kin.scheduler.core.master.domain.WorkerContext;

import java.util.List;

/**
 * application资源分配策略实现
 *
 * @author huangjianqin
 * @date 2020-03-03
 */
public interface AllocateStrategy {
    /**
     * @param workerContexts 可以分配资源的worker
     * @return 满足分配策略的worker资源
     */
    List<WorkerContext> allocate(List<WorkerContext> workerContexts);
}
