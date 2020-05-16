package org.kin.scheduler.core.driver.schedule;

import org.kin.scheduler.core.executor.transport.TaskExecResult;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public interface TaskExecCallback {
    void execFinish(TaskExecResult execResult);
}
