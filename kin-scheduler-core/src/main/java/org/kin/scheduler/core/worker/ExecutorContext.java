package org.kin.scheduler.core.worker;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.scheduler.core.executor.ExecutorBackend;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class ExecutorContext {
    private String executorId;
    private ReferenceConfig<ExecutorBackend> executorBackendReferenceConfig;
    private ExecutorBackend executorBackend;
    /** 内嵌在worker的Executor */
    private Worker.EmbeddedExecutorRunnable embeddedExecutorRunnable;

    public ExecutorContext(String executorId) {
        this(executorId, null);
    }

    public ExecutorContext(String executorId, Worker.EmbeddedExecutorRunnable embeddedExecutorRunnable) {
        this.executorId = executorId;
        this.embeddedExecutorRunnable = embeddedExecutorRunnable;
    }

    public void start(String executorBackendAddress) {
        executorBackendReferenceConfig = References.reference(ExecutorBackend.class)
                .appName("Worker-ExecutorBackend-".concat(executorId))
                .urls(executorBackendAddress);
        executorBackend = executorBackendReferenceConfig.get();
    }

    public void stop() {
        if (Objects.nonNull(executorBackendReferenceConfig)) {
            if (isEmbedded()) {
                embeddedExecutorRunnable.interrupt();
            } else {
                executorBackend.destroy();
            }
            executorBackendReferenceConfig.disable();
        }
    }

    //setter && getter

    public boolean isEmbedded() {
        return embeddedExecutorRunnable != null;
    }
}
