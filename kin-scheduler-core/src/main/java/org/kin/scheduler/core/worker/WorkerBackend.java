package org.kin.scheduler.core.worker;

import org.kin.scheduler.core.master.transport.ExecutorLaunchInfo;
import org.kin.scheduler.core.transport.RPCResult;
import org.kin.scheduler.core.worker.transport.ExecutorLaunchResult;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public interface WorkerBackend {
    /**
     * rpc请求启动executor
     *
     * @param launchInfo executor启动信息
     * @return executor启动结果
     */
    ExecutorLaunchResult launchExecutor(ExecutorLaunchInfo launchInfo);

    /**
     * rpc请求销毁executor
     *
     * @param executorId executorId
     */
    RPCResult shutdownExecutor(String executorId);
}
