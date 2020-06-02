package org.kin.scheduler.core.worker;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.scheduler.core.executor.ExecutorBackend;
import org.kin.scheduler.core.executor.transport.TaskSubmitResult;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.transport.RPCResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class ExecutorContext implements ExecutorBackend {
    private static final Logger log = LoggerFactory.getLogger(ExecutorContext.class);

    private String workerId;
    private String executorId;
    private ReferenceConfig<ExecutorBackend> executorBackendReferenceConfig;
    private ExecutorBackend executorBackend;

    public ExecutorContext(String workerId, String executorId) {
        this.workerId = workerId;
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
    public void destroy() {
        try {
            getExecutorBackend().destroy();
        } catch (Exception e) {
            log.warn("", e);
        } finally {
            executorBackendReferenceConfig.disable();
        }
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getExecutorId() {
        return executorId;
    }

    public String getExecutorAddress() {
        return executorBackendReferenceConfig.getRegistryConfig().getAddress();
    }
}
