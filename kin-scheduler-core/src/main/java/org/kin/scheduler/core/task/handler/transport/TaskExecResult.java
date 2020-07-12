package org.kin.scheduler.core.task.handler.transport;

import java.io.Serializable;

/**
 * task 执行结果
 *
 * @author huangjianqin
 * @date 2020-02-21
 */
public class TaskExecResult implements Serializable {
    private static final long serialVersionUID = 2270632692743128073L;
    public static final TaskExecResult NONE = TaskExecResult.of(null);

    /** task执行内容 */
    protected Serializable data;

    public static TaskExecResult of(Serializable data) {
        TaskExecResult execResult = new TaskExecResult();
        execResult.data = data;
        return execResult;
    }

    //setter && getter
    public Serializable getData() {
        return data;
    }

    public void setData(Serializable data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "TaskExecResult{" +
                "data=" + data +
                '}';
    }
}