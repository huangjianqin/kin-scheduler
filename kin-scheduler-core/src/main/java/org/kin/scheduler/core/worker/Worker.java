package org.kin.scheduler.core.worker;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.domain.WorkerRes;
import org.kin.scheduler.core.executor.Executor;
import org.kin.scheduler.core.executor.ExecutorBackend;
import org.kin.scheduler.core.executor.StandaloneExecutor;
import org.kin.scheduler.core.log.StaticLogger;
import org.kin.scheduler.core.master.MasterBackend;
import org.kin.scheduler.core.master.transport.ExecutorLaunchInfo;
import org.kin.scheduler.core.master.transport.WorkerRegisterResult;
import org.kin.scheduler.core.master.transport.WorkerUnregisterResult;
import org.kin.scheduler.core.transport.RPCResult;
import org.kin.scheduler.core.utils.LogUtils;
import org.kin.scheduler.core.utils.ScriptUtils;
import org.kin.scheduler.core.worker.transport.ExecutorLaunchResult;
import org.kin.scheduler.core.worker.transport.WorkerInfo;
import org.kin.scheduler.core.worker.transport.WorkerRegisterInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class Worker extends AbstractService implements WorkerBackend {
    private String workerId;
    /** worker配置 */
    private final Config config;
    /** RPC服务配置 */
    private ServiceConfig workerServiceConfig;
    /** RPC引用配置 */
    private ReferenceConfig<MasterBackend> masterBackendReferenceConfig;
    /** master 的rpc接口 */
    private MasterBackend masterBackend;
    /** executor 的rpc接口 */
    private Map<String, ExecutorContext> executors = new HashMap<>();
    /**
     * executorId counter
     * 类actor执行, 所有rpc请求都是同一线程处理, 不需要用原子类
     */
    private int executorIdCounter = 1;
    /** 已使用资源 */
    private WorkerRes res;
    /** embedded executor id 即使所属job已完成也不shutdown */
    private String embeddedExecutorId;

    public Worker(String workerId, Config config) {
        super("Worker-".concat(workerId));
        this.workerId = workerId;
        this.config = config;
        StaticLogger.init(config.getLogPath(), workerId);
    }

    @Override
    public void init() {
        super.init();
        masterBackendReferenceConfig = References.reference(MasterBackend.class)
                .appName(getName().concat("-MasterBackend"))
                .urls(NetUtils.getIpPort(config.getMasterBackendHost(), config.getMasterBackendPort()));
        masterBackend = masterBackendReferenceConfig.get();
        try {
            workerServiceConfig = Services.service(this, WorkerBackend.class)
                    .appName(getName())
                    .bind(config.getWorkerBackendHost(), config.getWorkerBackendPort())
                    .actorLike();
            workerServiceConfig.export();
        } catch (Exception e) {
            StaticLogger.log.error(e.getMessage(), e);
        }

        res = new WorkerRes(workerId);
    }

    @Override
    public void start() {
        super.start();
        //TODO 优化master失联情况
        //注册worker
        WorkerRegisterInfo registerInfo = generateWorkerRegisterInfo();
        try {
            WorkerRegisterResult registerResult = masterBackend.registerWorker(registerInfo);
            if (!registerResult.isSuccess()) {
                StaticLogger.log.error("worker register error >>> {}".concat(registerResult.getDesc()));
                stop();
            }
        } catch (Exception e) {
            StaticLogger.log.error("worker register encounter error >>> {}", e, e);
            stop();
        }

        JvmCloseCleaner.DEFAULT().add(JvmCloseCleaner.MAX_PRIORITY, this::stop);

        StaticLogger.log.info("worker({}) started", workerId);
    }

    @Override
    public void stop() {
        if (!isInState(State.STARTED)) {
            return;
        }
        super.stop();
        //取消注册
        try {
            WorkerUnregisterResult unregisterResult = masterBackend.unregisterWorker(workerId);
            if (!unregisterResult.isSuccess()) {
                StaticLogger.log.error("worker unregister error >>> {}", unregisterResult.getDesc());
            }
        } catch (Exception e) {
            StaticLogger.log.error("worker unregister encounter error >>> {}", e, e);
        }
        stop0();
    }

    private void stop0() {
        //关闭RPC 服务
        masterBackendReferenceConfig.disable();
        workerServiceConfig.disable();

        for (ExecutorContext executorContext : executors.values()) {
            executorContext.endStop();
        }
        executors.clear();

        StaticLogger.log.info("worker({}) closed", workerId);
    }

    private WorkerRegisterInfo generateWorkerRegisterInfo() {
        return new WorkerRegisterInfo(generateWorkerInfo());
    }

    private WorkerInfo generateWorkerInfo() {
        return new WorkerInfo(workerId, NetUtils.getIpPort(config.getWorkerBackendHost(), config.getWorkerBackendPort()),
                0, 0);
    }

    @Override
    public ExecutorLaunchResult launchExecutor(ExecutorLaunchInfo launchInfo) {
        ExecutorLaunchResult result = launchExecutor0(launchInfo);

        //log
        if (result.isSuccess()) {
            StaticLogger.log.info("lauch executor success >>> executorId({}), executorBackendAddress({})", result.getExecutorId(), result.getAddress());
        } else {
            StaticLogger.log.error("lauch executor fail >>> {}", result.getDesc());
        }

        return result;
    }

    private ExecutorLaunchResult launchExecutor0(ExecutorLaunchInfo launchInfo) {
        ExecutorLaunchResult result;
        Executor embeddedExecutor = null;

        int executorBackendPort = getAvailableExecutorBackendPort();
        if (executorBackendPort > 0) {
            if (config.isAllowEmbeddedExecutor()) {
                String executorId = workerId;
                boolean embeddedExecutorinited = StringUtils.isNotBlank(embeddedExecutorId);
                if (!embeddedExecutorinited) {
                    //尚未初始化embedded exeuctor
                    embeddedExecutor = new StandaloneExecutor(workerId, executorId, config.getLogPath(), config.getWorkerBackendHost(),
                            executorBackendPort, launchInfo.getExecutorDriverBackendAddress());
                    //启动内置Executor
                    Executor finalEmbeddedExecutor = embeddedExecutor;
                    Thread thread = new Thread(() -> {
                        finalEmbeddedExecutor.init();
                        finalEmbeddedExecutor.start();
                    }, getName().concat("-EmbeddedExecutorThread"));
                    thread.start();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                    embeddedExecutorId = executorId;
                }
                result = ExecutorLaunchResult.success(executorId, NetUtils.getIpPort(config.getWorkerBackendHost(), executorBackendPort));

                if (embeddedExecutorinited) {
                    //已初始化embedded exeuctor, 直接返回
                    return result;
                }
            } else {
                String executorId = workerId.concat("-Executor").concat(String.valueOf(executorIdCounter++));
                //启动新jvm来启动Executor
                int commandExecResult = ScriptUtils.execCommand("java -jar kin-scheduler-admin.jar ExecutorRunner",
                        LogUtils.getExecutorLogFileName(config.getLogPath(), workerId, executorId), "/",
                        workerId, String.valueOf(executorId), config.getWorkerBackendHost(), String.valueOf(executorBackendPort),
                        config.getLogPath(), launchInfo.getExecutorDriverBackendAddress());
                if (commandExecResult == 0) {
                    result = ExecutorLaunchResult.success(executorId, NetUtils.getIpPort(config.getWorkerBackendHost(), executorBackendPort));
                } else {
                    result = ExecutorLaunchResult.failure("executor launch fail");
                }
            }
        } else {
            result = ExecutorLaunchResult.failure("can not find available port for executor");
        }

        if (result.isSuccess()) {
            connectExecutor(result.getExecutorId(), result.getAddress(), embeddedExecutor);
        }

        return result;
    }

    private int getAvailableExecutorBackendPort() {
        int executorBackendPort;
        if (config.isAllowEmbeddedExecutor()) {
            executorBackendPort = config.getWorkerBackendPort();
        } else {
            executorBackendPort = config.getExecutorBackendPort();
            while (NetUtils.isPortInRange(executorBackendPort) && !NetUtils.isValidPort(executorBackendPort)) {
                executorBackendPort++;
            }
        }

        return NetUtils.isPortInRange(executorBackendPort) ? executorBackendPort : -1;
    }

    @Override
    public RPCResult shutdownExecutor(String executorId) {
        ExecutorContext executor = executors.remove(executorId);
        if (executor != null) {
            executor.destroy();
            return RPCResult.success();
        }
        return RPCResult.failure(String.format("unknown executor(id=%s)", executor));
    }

    private void connectExecutor(String executorId, String executorBackendAddress, Executor embeddedExecutor) {
        ExecutorContext executor = new ExecutorContext(executorId, embeddedExecutor);

        ReferenceConfig<ExecutorBackend> executorBackendReferenceConfig = References.reference(ExecutorBackend.class)
                .appName("Worker-ExecutorBackend-".concat(executorId))
                .urls(executorBackendAddress);

        executor.start(executorBackendReferenceConfig);

        executors.put(executorId, executor);
    }
}
