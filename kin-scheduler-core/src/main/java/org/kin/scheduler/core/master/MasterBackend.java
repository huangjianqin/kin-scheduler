package org.kin.scheduler.core.master;

import org.kin.scheduler.core.master.transport.WorkerRegisterResult;
import org.kin.scheduler.core.master.transport.WorkerUnregisterResult;
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
     * @return 注销worker结果
     */
    WorkerUnregisterResult unregisterWorker(String workerId);

}
