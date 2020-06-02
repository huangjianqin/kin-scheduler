package org.kin.scheduler.core.executor;

import org.kin.scheduler.core.executor.transport.TaskSubmitResult;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.transport.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * 暴露给Worker使用的rpc接口
 */
public interface ExecutorBackend {
    /**
     * rpc请求执行task
     *
     * @param taskDescription task信息
     * @return task执行结果
     */
    TaskSubmitResult execTask(TaskDescription taskDescription);

    /**
     * rpc请求取消task
     *
     * @param taskId taskId
     * @return task取消结果
     */
    RPCResult cancelTask(String taskId);

    /**
     * 销毁executor
     */
    void destroy();
}
