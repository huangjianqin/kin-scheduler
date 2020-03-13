package org.kin.scheduler.core.driver;

import org.kin.scheduler.core.executor.domain.TaskExecResult;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public interface TaskExecCallback {
    void execFinish(TaskExecResult execResult);
}
