package org.kin.scheduler.core.worker;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.keeper.Keeper;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.executor.Executor;
import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;
import org.kin.scheduler.core.log.StaticLogger;
import org.kin.scheduler.core.master.MasterBackend;
import org.kin.scheduler.core.master.transport.ExecutorLaunchInfo;
import org.kin.scheduler.core.master.transport.WorkerHeartbeatResp;
import org.kin.scheduler.core.master.transport.WorkerRegisterResult;
import org.kin.scheduler.core.master.transport.WorkerUnregisterResult;
import org.kin.scheduler.core.utils.LogUtils;
import org.kin.scheduler.core.utils.ScriptUtils;
import org.kin.scheduler.core.worker.transport.ExecutorLaunchResult;
import org.kin.scheduler.core.worker.transport.WorkerHeartbeat;
import org.kin.scheduler.core.worker.transport.WorkerInfo;
import org.kin.scheduler.core.worker.transport.WorkerRegisterInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class Worker extends AbstractService implements WorkerBackend, ExecutorWorkerBackend {
    private String workerId;
    /** worker配置 */
    private final Config config;
    /** RPC服务配置 */
    private ServiceConfig workerServiceConfig;
    /** executor -> worker RPC服务配置 */
    private ServiceConfig executorWorkerBackendServiceConfig;
    /** RPC引用配置 */
    private ReferenceConfig<MasterBackend> masterBackendReferenceConfig;
    /** master 的rpc接口 */
    private MasterBackend masterBackend;
    /** executor 的rpc接口 仅仅保存引用, 并不会操作实例 */
    private Map<String, Executor> embeddedExecutors = new HashMap<>();
    /**
     * executorId counter
     * 类actor执行, 所有rpc请求都是同一线程处理, 不需要用原子类
     */
    private int executorIdCounter = 1;
    //定时发送心跳
    private Keeper.KeeperStopper heartbeatKeeper;

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

        try {
            executorWorkerBackendServiceConfig = Services.service(this, ExecutorWorkerBackend.class)
                    .appName(getName())
                    .bind(config.getWorkerBackendHost(), config.getWorkerBackendPort())
                    .actorLike();
            executorWorkerBackendServiceConfig.export();
        } catch (Exception e) {
            StaticLogger.log.error(e.getMessage(), e);
        }
    }

    private WorkerRegisterInfo generateWorkerRegisterInfo() {
        return new WorkerRegisterInfo(generateWorkerInfo());
    }

    private WorkerInfo generateWorkerInfo() {
        return new WorkerInfo(workerId, NetUtils.getIpPort(config.getWorkerBackendHost(), config.getWorkerBackendPort()),
                config.getCpuCore(), 0);
    }

    private void registerWorker() {
        //注册worker
        WorkerRegisterResult registerResult = masterBackend.registerWorker(generateWorkerRegisterInfo());
        if (!registerResult.isSuccess()) {
            StaticLogger.log.error("worker register error >>> {}".concat(registerResult.getDesc()));
        }
    }

    @Override
    public void start() {
        super.start();

        registerWorker();

        heartbeatKeeper = Keeper.keep(this::sendHeartbeat);

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
        if (Objects.nonNull(heartbeatKeeper)) {
            heartbeatKeeper.stop();
        }
        //关闭RPC 服务
        masterBackendReferenceConfig.disable();
        workerServiceConfig.disable();
        executorWorkerBackendServiceConfig.disable();

        StaticLogger.log.info("worker({}) closed", workerId);
    }

    private void sendHeartbeat() {
        long heartbeatTime = config.getHeartbeatTime();
        try {
            long sleepTime = heartbeatTime - TimeUtils.timestamp() % heartbeatTime;
            if (sleepTime > 0 && sleepTime < heartbeatTime) {
                TimeUnit.SECONDS.sleep(sleepTime);
            }
        } catch (InterruptedException e) {

        }

        WorkerHeartbeatResp workerHeartbeatResp = masterBackend.workerHeartbeat(new WorkerHeartbeat(workerId));
        if (workerHeartbeatResp.isReconnect()) {
            //该worker还没注册, master通知注册
            registerWorker();
        }
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
        if (!isInState(State.STARTED)) {
            return ExecutorLaunchResult.failure("worker not start");
        }

        ExecutorLaunchResult result;

        String appName = launchInfo.getAppName();
        String executorWorkerBackendAddress = NetUtils.getIpPort(config.getWorkerBackendHost(), config.getWorkerBackendPort());
        int executorBackendPort = getAvailableExecutorBackendPort();
        if (executorBackendPort > 0) {
            String executorId = workerId.concat("-Executor-").concat(String.valueOf(executorIdCounter++));
            if (config.isAllowEmbeddedExecutor()) {
                Executor executor = new Executor(appName, workerId, executorId, config.getWorkerBackendHost(), executorBackendPort, config.getLogPath(),
                        launchInfo.getExecutorDriverBackendAddress(), executorWorkerBackendAddress, true);
                executor.init();
                executor.start();

                embeddedExecutors.put(executorId, executor);

                result = ExecutorLaunchResult.success(executorId, NetUtils.getIpPort(config.getWorkerBackendHost(), executorBackendPort));
            } else {
                //启动新jvm来启动Executor
                //TODO 通过启动进程控制CPU使用数
                int commandExecResult = ScriptUtils.execCommand("java -jar kin-scheduler-admin.jar ExecutorRunner",
                        LogUtils.getExecutorLogFileName(config.getLogPath(), workerId, executorId), "/",
                        appName, workerId, executorId, config.getWorkerBackendHost(), String.valueOf(executorBackendPort),
                        config.getLogPath(), launchInfo.getExecutorDriverBackendAddress(), executorWorkerBackendAddress);
                if (commandExecResult == 0) {
                    result = ExecutorLaunchResult.success(executorId, NetUtils.getIpPort(config.getWorkerBackendHost(), executorBackendPort));
                } else {
                    result = ExecutorLaunchResult.failure("executor launch fail");
                }
            }
        } else {
            result = ExecutorLaunchResult.failure("can not find available port for executor");
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
    public void reconnect2Master() {
        registerWorker();
    }

    @Override
    public void executorStateChanged(ExecutorStateChanged executorStateChanged) {
        if (!isInState(State.STARTED)) {
            return;
        }

        embeddedExecutors.remove(executorStateChanged.getExecutorId());
        masterBackend.executorStateChanged(executorStateChanged);
    }
}
