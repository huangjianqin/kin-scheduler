package org.kin.scheduler.core.worker;

import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;

/**
 * @author huangjianqin
 * @date 2020-05-25
 */
public interface ExecutorWorkerBackend {
    /**
     * executor状态变化
     *
     * @param executorState executor状态信息
     */
    void executorStateChanged(ExecutorStateChanged executorState);
}
