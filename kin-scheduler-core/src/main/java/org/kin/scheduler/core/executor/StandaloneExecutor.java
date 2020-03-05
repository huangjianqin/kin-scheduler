package org.kin.scheduler.core.executor;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-03-05
 * <p>
 * 单独在一个jvm上运行的executor
 */
public class StandaloneExecutor extends Executor {
    /** Executor暴露给worker的host */
    private String backendHost;
    /** Executor暴露给worker的端口 */
    private int backendPort;
    /** rpc服务配置 */
    private ServiceConfig serviceConfig;

    public StandaloneExecutor(String workerId, String executorId, String backendHost, int backendPort) {
        super(workerId, executorId);
        this.backendHost = backendHost;
        this.backendPort = backendPort;
    }

    public StandaloneExecutor(String workerId, String executorId, String logPath, String backendHost, int backendPort) {
        super(workerId, executorId, logPath);
        this.backendHost = backendHost;
        this.backendPort = backendPort;
    }

    @Override
    public void init() {
        super.init();
        try {
            serviceConfig = Services.service(this, ExecutorBackend.class)
                    .appName(getName())
                    .actorLike()
                    .bind(backendHost, backendPort);
            serviceConfig.export();
        } catch (Exception e) {
            //TODO
            System.exit(-1);
        }
    }

    @Override
    public void close() {
        if (Objects.nonNull(serviceConfig)) {
            serviceConfig.disable();
        }
        super.close();
    }
}
