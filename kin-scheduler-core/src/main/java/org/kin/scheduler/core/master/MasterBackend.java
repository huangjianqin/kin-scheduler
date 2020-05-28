package org.kin.scheduler.core.master;

import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;
import org.kin.scheduler.core.master.transport.WorkerHeartbeatResp;
import org.kin.scheduler.core.master.transport.WorkerRegisterResult;
import org.kin.scheduler.core.worker.transport.WorkerHeartbeat;
import org.kin.scheduler.core.worker.transport.WorkerRegisterInfo;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public interface MasterBackend {
    /**
     * 注册worker, 成功注册的worker才是有效的worker
     *
     * @param registerInfo worker信息
     * @return worker注册结果
     */
    WorkerRegisterResult registerWorker(WorkerRegisterInfo registerInfo);

    /**
     * 注销worker
     *
     * @param workerId workerId
     */
    void unregisterWorker(String workerId);

    /**
     * 定时往master发送心跳
     * 1. 移除超时worker
     * 2. 发现心跳worker还没注册, 通知其注册
     */
    WorkerHeartbeatResp workerHeartbeat(WorkerHeartbeat heartbeat);

    /**
     * executor状态变化
     *
     * @param executorState executor状态信息
     */
    void executorStateChanged(ExecutorStateChanged executorStateChanged);
}
