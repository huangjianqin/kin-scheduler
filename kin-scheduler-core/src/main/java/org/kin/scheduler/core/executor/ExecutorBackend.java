package org.kin.scheduler.core.executor;

import org.kin.scheduler.core.domain.RPCResult;
import org.kin.scheduler.core.executor.domain.TaskExecResult;
import org.kin.scheduler.core.task.Task;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * 暴露给Worker使用的rpc接口
 */
public interface ExecutorBackend {
    /**
     * rpc请求执行task
     */
    TaskExecResult execTask(Task task);

    /**
     * rpc请求取消task
     */
    RPCResult cancelTask(String taskId);

    /**
     * 销毁executor
     */
    void destroy();
}
