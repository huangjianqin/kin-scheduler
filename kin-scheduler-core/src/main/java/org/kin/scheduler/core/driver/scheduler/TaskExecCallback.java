package org.kin.scheduler.core.driver.scheduler;

import org.kin.scheduler.core.driver.transport.TaskExecResult;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public interface TaskExecCallback {
    void execFinish(TaskExecResult execResult);
}
