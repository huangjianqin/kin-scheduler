package org.kin.scheduler.core.worker;

import org.kin.scheduler.core.worker.domain.ExecutorLaunchInfo;
import org.kin.scheduler.core.worker.domain.ExecutorLaunchResult;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public interface WorkerBackend {
    /**
     * rpc请求启动executor
     */
    ExecutorLaunchResult launchExecutor(ExecutorLaunchInfo launchInfo);

    /**
     * rpc请求销毁executor
     */
    void shutdownExecutor(String executorId);
}
