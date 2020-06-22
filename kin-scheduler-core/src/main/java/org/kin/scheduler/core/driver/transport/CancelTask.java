package org.kin.scheduler.core.driver.transport;

import java.io.Serializable;

/**
 * 取消task 消息
 *
 * @author huangjianqin
 * @date 2020-06-18
 */
public class CancelTask implements Serializable {
    private static final long serialVersionUID = -6342519306375083927L;
    /** task id */
    private String taskId;

    public static CancelTask of(String taskId) {
        CancelTask message = new CancelTask();
        message.taskId = taskId;
        return message;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
