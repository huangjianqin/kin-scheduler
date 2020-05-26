package org.kin.scheduler.core.driver;

import org.kin.scheduler.core.driver.transport.ExecutorRegisterInfo;
import org.kin.scheduler.core.driver.transport.TaskExecResult;
import org.kin.scheduler.core.transport.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public interface SchedulerBackend {
    /**
     * @param executorRegisterInfo
     * @return
     */
    RPCResult registerExecutor(ExecutorRegisterInfo executorRegisterInfo);

    /**
     *
     * @param execResult
     */
    void taskFinish(TaskExecResult execResult);
}
