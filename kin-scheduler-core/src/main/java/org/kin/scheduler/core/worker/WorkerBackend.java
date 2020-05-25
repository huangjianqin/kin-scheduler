package org.kin.scheduler.core.worker;

import org.kin.scheduler.core.master.transport.ExecutorLaunchInfo;
import org.kin.scheduler.core.worker.transport.ExecutorLaunchResult;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public interface WorkerBackend {
    /**
     * 发现心跳worker还没注册, master通知worker重新注册
     */
    void reconnect2Master();

    /**
     * rpc请求启动executor
     *
     * @param launchInfo executor启动信息
     * @return executor启动结果
     */
    ExecutorLaunchResult launchExecutor(ExecutorLaunchInfo launchInfo);

}
