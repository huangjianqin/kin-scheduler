package org.kin.scheduler.core.executor;

import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.driver.ExecutorDriverBackend;
import org.kin.scheduler.core.driver.domain.ExecutorRegisterInfo;
import org.kin.scheduler.core.transport.RPCResult;

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
    private String driverAddress;
    /** driver服务配置 */
    private ReferenceConfig<ExecutorDriverBackend> executorDriverBackendReferenceConfig;

    public StandaloneExecutor(String workerId, String executorId, String backendHost, int backendPort, String driverAddress) {
        super(workerId, executorId);
        this.backendHost = backendHost;
        this.backendPort = backendPort;
        this.driverAddress = driverAddress;
    }

    public StandaloneExecutor(String workerId, String executorId, String logPath, String backendHost, int backendPort, String driverAddress) {
        super(workerId, executorId, logPath);
        this.backendHost = backendHost;
        this.backendPort = backendPort;
        this.driverAddress = driverAddress;
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
            log.error(e.getMessage(), e);
        }
        executorDriverBackendReferenceConfig = References.reference(ExecutorDriverBackend.class)
                .appName(getName().concat("-ExecutorDriverBackendReference"))
                .urls(driverAddress);
        executorDriverBackend = executorDriverBackendReferenceConfig.get();
    }

    @Override
    public void start() {
        RPCResult result = executorDriverBackend.registerExecutor(new ExecutorRegisterInfo(executorId, NetUtils.getIpPort(backendHost, backendPort)));
        if (!result.isSuccess()) {
            log.error("executor register driver error >>> {}", result.getDesc());
        }
        super.start();
    }

    @Override
    public void stop() {
        if (Objects.nonNull(serviceConfig)) {
            serviceConfig.disable();
        }
        executorDriverBackendReferenceConfig.disable();
        super.stop();
    }
}
