package org.kin.scheduler.core.worker;

import ch.qos.logback.classic.Logger;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.keeper.Keeper;
import org.kin.framework.utils.CommandUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.RpcMessageCallContext;
import org.kin.kinrpc.message.core.ThreadSafeRpcEndpoint;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.driver.transport.ReadFile;
import org.kin.scheduler.core.executor.Executor;
import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;
import org.kin.scheduler.core.log.LogUtils;
import org.kin.scheduler.core.log.Loggers;
import org.kin.scheduler.core.master.Master;
import org.kin.scheduler.core.master.transport.LaunchExecutor;
import org.kin.scheduler.core.master.transport.RegisterWorkerResp;
import org.kin.scheduler.core.master.transport.UnRegisterWorkerResp;
import org.kin.scheduler.core.master.transport.WorkerReRegister;
import org.kin.scheduler.core.worker.transport.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class Worker extends ThreadSafeRpcEndpoint {
    private Logger log;

    private final String workerId;
    /** worker配置 */
    private final Config config;
    /** master client */
    private final RpcEndpointRef masterRef;
    /** executor 的rpc接口 仅仅保存引用, 并不会操作实例 */
    private Map<String, Executor> embeddedExecutors = new HashMap<>();
    /**
     * executorId counter
     * 类actor执行, 所有rpc请求都是同一线程处理, 不需要用原子类
     */
    private int executorIdCounter = 1;
    //定时发送心跳
    private Keeper.KeeperStopper heartbeatKeeper;
    private volatile boolean isStopped;

    public Worker(RpcEnv rpcEnv, String workerId, Config config) {
        super(rpcEnv);
        this.workerId = workerId;
        this.config = config;
        this.masterRef = rpcEnv.createEndpointRef(config.getMasterBackendHost(), config.getMasterBackendPort(), Master.DEFAULT_NAME);
        log = Loggers.worker(config.getLogPath(), workerId);

        JvmCloseCleaner.DEFAULT().add(JvmCloseCleaner.MAX_PRIORITY, this::stop);
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerWorker();

        heartbeatKeeper = Keeper.keep(this::sendHeartbeat);

        log.info("worker({}) started", workerId);
    }

    @Override
    protected void onStop() {
        super.onStop();

        isStopped = true;
        //取消注册
        unRegisterWorker();
        if (Objects.nonNull(heartbeatKeeper)) {
            heartbeatKeeper.stop();
        }
        log.info("worker({}) stopped", workerId);
    }

    @Override
    public void receive(RpcMessageCallContext context) {
        super.receive(context);

        Serializable message = context.getMessage();
        if (message instanceof RegisterWorkerResp) {
            registerWorkerResp((RegisterWorkerResp) message);
        } else if (message instanceof UnRegisterWorkerResp) {
            unRegisterWorkerResp();
        } else if (message instanceof LaunchExecutor) {
            launchExecutor((LaunchExecutor) message);
        } else if (message instanceof ReadFile) {
            readFile(context, (ReadFile) message);
        } else if (message instanceof WorkerReRegister) {
            registerWorker();
        } else if (message instanceof ExecutorStateChanged) {
            executorStateChanged((ExecutorStateChanged) message);
        }
    }

    public void start() {
        if (isStopped) {
            return;
        }
        rpcEnv.register(workerId, this);
    }

    public void stop() {
        if (isStopped) {
            return;
        }
        rpcEnv.unregister(workerId, this);
    }

    //---------------------------------------WorkerBackend-------------------------------------------------------------------------

    private void registerWorker() {
        if (isStopped) {
            return;
        }
        //注册worker
        masterRef.send(RegisterWorker.of(WorkerInfo.of(workerId, config.getCpuCore(), 0), ref()));
    }

    private void registerWorkerResp(RegisterWorkerResp registerWorkerResp) {
        if (isStopped) {
            return;
        }
        if (!registerWorkerResp.isSuccess()) {
            log.error("worker '{}' register error >>> {}", workerId, registerWorkerResp.getDesc());
        } else {
            log.info("worker '{}' registered", workerId);
        }
    }

    private void unRegisterWorker() {
        if (isStopped) {
            return;
        }
        //取消注册worker
        masterRef.send(UnRegisterWorker.of(workerId));
    }

    private void unRegisterWorkerResp() {
        if (isStopped) {
            return;
        }
        //取消注册worker
        log.info("worker '{}' unRegistered", workerId);
    }

    private void sendHeartbeat() {
        if (isStopped) {
            return;
        }
        long heartbeatTime = config.getHeartbeatTime();
        try {
            long sleepTime = heartbeatTime - System.currentTimeMillis() % heartbeatTime;
            if (sleepTime > 0 && sleepTime < heartbeatTime) {
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            return;
        }

        masterRef.send(WorkerHeartbeat.of(workerId, ref()));
    }

    private void launchExecutor(LaunchExecutor launchInfo) {
        if (isStopped) {
            return;
        }
        LaunchExecutorResp result = launchExecutor0(launchInfo);

        //log
        if (result.isSuccess()) {
            log.info("lauch executor success >>> executorId({}), executorBackendAddress({})", result.getExecutorId(), result.getAddress());
        } else {
            log.error("lauch executor fail >>> {}", result.getDesc());
        }

        masterRef.send(result);
    }

    private LaunchExecutorResp launchExecutor0(LaunchExecutor launchInfo) {
        if (isStopped) {
            return LaunchExecutorResp.failure("worker not start");
        }

        LaunchExecutorResp result;

        String appName = launchInfo.getAppName();
        int cpuCore = launchInfo.getCpuCore();

        String executorWorkerBackendAddress = NetUtils.getIpPort(config.getWorkerBackendHost(), config.getWorkerBackendPort());
        int executorBackendPort = getAvailableExecutorBackendPort();
        if (executorBackendPort > 0) {
            String executorId = workerId.concat("-Executor-").concat(String.valueOf(executorIdCounter++));
            if (config.isAllowEmbeddedExecutor()) {
                Executor executor = new Executor(rpcEnv, appName, workerId, executorId, config.getLogPath(),
                        launchInfo.getExecutorDriverBackendAddress(), executorWorkerBackendAddress, true);
                executor.start();

                embeddedExecutors.put(executorId, executor);

                result = LaunchExecutorResp.success(appName, executorId, workerId, cpuCore,
                        NetUtils.getIpPort(config.getWorkerBackendHost(), executorBackendPort));
            } else {
                //启动新jvm来启动Executor
                //TODO 通过启动进程控制CPU使用数
                int commandExecResult = 0;
                String reason = "";
                try {
                    commandExecResult = CommandUtils.execCommand("java -jar kin-scheduler-admin.jar ExecutorRunner",
                            LogUtils.getExecutorLogFileName(config.getLogPath(), workerId, executorId), "/",
                            appName, workerId, executorId, config.getWorkerBackendHost(), String.valueOf(executorBackendPort),
                            config.getLogPath(), launchInfo.getExecutorDriverBackendAddress(), executorWorkerBackendAddress);
                } catch (Exception e) {
                    reason = ExceptionUtils.getExceptionDesc(e);
                }
                if (commandExecResult == 0) {
                    result = LaunchExecutorResp.success(appName, executorId, workerId, cpuCore,
                            NetUtils.getIpPort(config.getWorkerBackendHost(), executorBackendPort));
                } else {
                    result = LaunchExecutorResp.failure("executor launch fail >>>>>".concat(System.lineSeparator()).concat(reason));
                }
            }
        } else {
            result = LaunchExecutorResp.failure("can not find available port for executor");
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


    private void readFile(RpcMessageCallContext context, ReadFile readFile) {
        if (isStopped) {
            return;
        }
        String path = readFile.getPath();
        int fromLineNum = readFile.getFromLineNum();

        if (StringUtils.isBlank(path)) {
            context.reply(TaskExecFileContent.fail(workerId, path, fromLineNum, "path is blank"));
            return;
        }

        File logFile = new File(path);
        if (!logFile.exists()) {
            context.reply(TaskExecFileContent.fail(workerId, path, fromLineNum, "read file fail, file not found"));
            return;
        }

        StringBuffer logContentBuffer = new StringBuffer();
        int toLineNum = 0;
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                toLineNum = reader.getLineNumber();
                if (toLineNum >= fromLineNum) {
                    logContentBuffer.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            log.error("read log file encounter error >>>>", e);
            context.reply(TaskExecFileContent.fail(workerId, path, fromLineNum, ExceptionUtils.getExceptionDesc(e)));
            return;
        }
        context.reply(TaskExecFileContent.success(workerId, path, fromLineNum, toLineNum, logContentBuffer.toString(), fromLineNum == toLineNum));
    }

    private void executorStateChanged(ExecutorStateChanged executorStateChanged) {
        if (isStopped) {
            return;
        }
        masterRef.send(executorStateChanged);
        if (executorStateChanged.getState().isFinished()) {
            Executor invalidExecutor = embeddedExecutors.remove(executorStateChanged.getExecutorId());
            invalidExecutor.stop();
        }
    }
}
