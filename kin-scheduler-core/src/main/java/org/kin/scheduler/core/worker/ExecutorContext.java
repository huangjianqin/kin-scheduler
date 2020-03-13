package org.kin.scheduler.core.worker;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.scheduler.core.domain.RPCResult;
import org.kin.scheduler.core.executor.Executor;
import org.kin.scheduler.core.executor.ExecutorBackend;
import org.kin.scheduler.core.executor.domain.TaskExecLog;
import org.kin.scheduler.core.executor.domain.TaskSubmitResult;
import org.kin.scheduler.core.task.Task;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class ExecutorContext implements ExecutorBackend {
    private String executorId;
    private ReferenceConfig<ExecutorBackend> executorBackendReferenceConfig;
    private ExecutorBackend executorBackend;
    /** 内嵌在worker的Executor */
    private Executor embeddedExecutor;

    public ExecutorContext(String executorId) {
        this(executorId, null);
    }

    public ExecutorContext(String executorId, Executor embeddedExecutor) {
        this.executorId = executorId;
        this.embeddedExecutor = embeddedExecutor;
    }

    public void start(ReferenceConfig<ExecutorBackend> referenceConfig) {
        if (!isEmbedded()) {
            this.executorBackendReferenceConfig = referenceConfig;
            executorBackend = executorBackendReferenceConfig.get();
        }
    }

    /**
     * worker end时调用
     */
    public void endStop() {
        getExecutorBackend().destroy();
    }

    private ExecutorBackend getExecutorBackend() {
        if (isEmbedded()) {
            return embeddedExecutor;
        } else {
            return executorBackend;
        }
    }

    @Override
    public TaskSubmitResult execTask(Task task) {
        return getExecutorBackend().execTask(task);
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
        destroy(true);
    }

    public void destroy(boolean killExecutor) {
        //仅仅处理standalone executor
        if (Objects.nonNull(executorBackendReferenceConfig)) {
            if (!isEmbedded()) {
                if (killExecutor) {
                    executorBackend.destroy();
                }
                executorBackendReferenceConfig.disable();
            }
        }
    }

    public boolean isEmbedded() {
        return embeddedExecutor != null;
    }

    public String getExecutorId() {
        return executorId;
    }

    public String getExecutorAddress() {
        return isEmbedded() ? "embedded" : executorBackendReferenceConfig.getRegistryConfig().getAddress();
    }
}
