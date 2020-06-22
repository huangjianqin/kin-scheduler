package org.kin.scheduler.core.driver.scheduler;

import org.kin.scheduler.core.task.domain.TaskStatus;

import java.io.Serializable;

/**
 * task执行完成callback
 *
 * @author huangjianqin
 * @date 2020-03-09
 */
public interface TaskExecCallback<R extends Serializable> {
    /**
     * @param taskId     taskId
     * @param taskStatus task状态
     * @param result     task执行结果
     * @param logPath    task log路径
     * @param outputPath task output路径
     * @param reason     task fail原因
     */
    void execFinish(String taskId, TaskStatus taskStatus, R result, String logPath, String outputPath, String reason);
}
