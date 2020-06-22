package org.kin.scheduler.core.driver.exception;

/**
 * @author huangjianqin
 * @date 2020-06-21
 */
public class TaskRepeatSubmitException extends RuntimeException {
    public TaskRepeatSubmitException(String jobId, String taskId) {
        super(String.format("job '%s's task '%s' repeat submit", jobId, taskId));
    }
}
