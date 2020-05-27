package org.kin.scheduler.core.driver;

import org.kin.scheduler.core.driver.transport.ExecutorRegisterInfo;
import org.kin.scheduler.core.driver.transport.TaskStatusChanged;
import org.kin.scheduler.core.transport.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public interface SchedulerBackend {
    /**
     * 注册executor
     * @param  executorRegisterInfo executor注册信息
     */
    RPCResult registerExecutor(ExecutorRegisterInfo executorRegisterInfo);

    /**
     * task 状态变化
     * @param taskStatusChanged task状态信息
     */
    void taskStatusChange(TaskStatusChanged taskStatusChanged);
}
