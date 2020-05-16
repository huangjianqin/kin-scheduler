package org.kin.scheduler.core.driver;

import org.kin.scheduler.core.driver.domain.ExecutorRegisterInfo;
import org.kin.scheduler.core.executor.transport.TaskExecResult;
import org.kin.scheduler.core.transport.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public interface ExecutorDriverBackend {
    RPCResult registerExecutor(ExecutorRegisterInfo executorRegisterInfo);

    void taskFinish(TaskExecResult execResult);
}
