package org.kin.scheduler.core.driver.transport;

import org.kin.scheduler.core.task.TaskDescription;

import java.io.Serializable;

/**
 * task submit消息
 *
 * @author huangjianqin
 * @date 2020-06-18
 */
public class SubmitTask implements Serializable {
    private static final long serialVersionUID = -7232876834490326118L;
    /** task描述 */
    private TaskDescription taskDescription;

    public static SubmitTask of(TaskDescription taskDescription) {
        SubmitTask message = new SubmitTask();
        message.taskDescription = taskDescription;
        return message;
    }

    public TaskDescription getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(TaskDescription taskDescription) {
        this.taskDescription = taskDescription;
    }
}
