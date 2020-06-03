package org.kin.scheduler.core.driver.scheduler;

import org.kin.scheduler.core.task.domain.TaskStatus;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public interface TaskExecCallback<R extends Serializable> {
    void execFinish(String taskId, TaskStatus taskStatus, R result, String logPath, String outputPath, String reason);
}
