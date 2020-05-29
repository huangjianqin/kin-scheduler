package org.kin.scheduler.core.worker;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.scheduler.core.executor.ExecutorBackend;
import org.kin.scheduler.core.executor.transport.TaskExecLog;
import org.kin.scheduler.core.executor.transport.TaskSubmitResult;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.transport.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class ExecutorContext implements ExecutorBackend {
    private String executorId;
    private ReferenceConfig<ExecutorBackend> executorBackendReferenceConfig;
    private ExecutorBackend executorBackend;

    public ExecutorContext(String executorId) {
        this.executorId = executorId;
    }

    public void start(ReferenceConfig<ExecutorBackend> referenceConfig) {
        this.executorBackendReferenceConfig = referenceConfig;
        executorBackend = executorBackendReferenceConfig.get();
    }

    private ExecutorBackend getExecutorBackend() {
        return executorBackend;
    }

    @Override
    public TaskSubmitResult execTask(TaskDescription taskDescription) {
        return getExecutorBackend().execTask(taskDescription);
    }

    @Override
    public RPCResult cancelTask(String taskId) {
        return getExecutorBackend().cancelTask(taskId);
    }

    @Override
    public TaskExecLog readLog(String logPath, int fromLineNum) {
        return getExecutorBackend().readLog(logPath, fromLineNum);
    }

    @Override
    public void destroy() {
        getExecutorBackend().destroy();
        executorBackendReferenceConfig.disable();
    }

    public String getExecutorId() {
        return executorId;
    }

    public String getExecutorAddress() {
        return executorBackendReferenceConfig.getRegistryConfig().getAddress();
    }
}
