package org.kin.scheduler.core.driver;

import org.kin.scheduler.core.domain.RPCResult;
import org.kin.scheduler.core.driver.domain.ExecutorRegisterInfo;
import org.kin.scheduler.core.executor.domain.TaskExecResult;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public interface ExecutorDriverBackend {
    RPCResult registerExecutor(ExecutorRegisterInfo executorRegisterInfo);

    void taskFinish(TaskExecResult execResult);
}
