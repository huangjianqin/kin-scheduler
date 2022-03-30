package org.kin.scheduler.core.worker;

import ch.qos.logback.classic.Logger;
import org.kin.framework.concurrent.Keeper;
import org.kin.framework.utils.CommandUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.message.core.MessagePostContext;
import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.ThreadSafeRpcEndpoint;
import org.kin.kinrpc.message.core.message.ClientDisconnected;
import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.driver.transport.ReadFile;
import org.kin.scheduler.core.executor.Executor;
import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;
import org.kin.scheduler.core.log.Loggers;
import org.kin.scheduler.core.master.Master;
import org.kin.scheduler.core.master.transport.LaunchExecutor;
import org.kin.scheduler.core.master.transport.RegisterWorkerResp;
import org.kin.scheduler.core.master.transport.WorkerReRegister;
import org.kin.scheduler.core.worker.domain.WorkerInfo;
import org.kin.scheduler.core.worker.transport.RegisterWorker;
import org.kin.scheduler.core.worker.transport.TaskExecFileContent;
import org.kin.scheduler.core.worker.transport.WorkerHeartbeat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
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
    /** 定时发送心跳keeper */
    private Keeper.KeeperStopper heartbeatKeeper;
    private volatile boolean isStopped;
    /** 是否已注册 */
    private volatile boolean registered;

    public Worker(RpcEnv rpcEnv, String workerId, Config config) {
        super(rpcEnv);
        this.workerId = workerId;
        this.config = config;
        this.masterRef = rpcEnv.createEndpointRef(config.getMasterHost(), config.getMasterPort(), Master.DEFAULT_NAME);
        log = Loggers.worker(config.getLogPath(), workerId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //注册worker
        registerWorker();
        //启动定时心跳
        heartbeatKeeper = Keeper.keep(this::sendHeartbeat);
        log.info("worker({}) started on {}", workerId, rpcEnv.address());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isStopped) {
            return;
        }
        isStopped = true;
        //关闭内嵌executor
        for (Executor executor : embeddedExecutors.values()) {
            executor.stop();
        }
        //TODO 关闭进程级executor
        if (Objects.nonNull(heartbeatKeeper)) {
            heartbeatKeeper.stop();
        }
        log.info("worker({}) stopped", workerId);
    }

    @Override
    public void onReceiveMessage(MessagePostContext context) {
        Serializable message = context.getMessage();
        if (message instanceof RegisterWorkerResp) {
            registerWorkerResp((RegisterWorkerResp) message);
        } else if (message instanceof LaunchExecutor) {
            launchExecutor((LaunchExecutor) message);
        } else if (message instanceof ReadFile) {
            readFile(context, (ReadFile) message);
        } else if (message instanceof WorkerReRegister) {
            registerWorker();
        } else if (message instanceof ExecutorStateChanged) {
            executorStateChanged((ExecutorStateChanged) message);
        } else if (message instanceof ClientDisconnected) {
            remoteDisconnected(((ClientDisconnected) message).getRpcAddress());
        }
    }

    public void createEndpoint() {
        if (isStopped) {
            return;
        }
        rpcEnv.register(workerId, this);
    }

    //---------------------------------------Worker endpoint-------------------------------------------------------------------------

    /**
     * 通知master注册worker
     */
    private void registerWorker() {
        if (isStopped || registered) {
            return;
        }
        //注册worker
        masterRef.fireAndForget(RegisterWorker.of(WorkerInfo.of(workerId, config.getCpuCore(), 0, config.isAllowEmbeddedExecutor()), ref()));
    }

    /**
     * worker注册返回
     */
    private void registerWorkerResp(RegisterWorkerResp registerWorkerResp) {
        if (isStopped) {
            return;
        }
        if (registerWorkerResp.isSuccess()) {
            log.info("worker '{}' registered >>>>> cpu:{}, memory:{}", workerId, config.getCpuCore(), 0);
            registered = true;
        } else {
            log.error("worker '{}' register error >>> {}", workerId, registerWorkerResp.getDesc());
        }
    }

    /**
     * worker定时发送心跳逻辑
     */
    private void sendHeartbeat() {
        if (isStopped || !registered) {
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

        masterRef.fireAndForget(WorkerHeartbeat.of(workerId, ref()));
    }

    private String appExecutorId(String appName, String executorId) {
        return appName.concat("/").concat(executorId);
    }

    /**
     * master通知worker启动executor
     */
    private void launchExecutor(LaunchExecutor launchInfo) {
        if (isStopped) {
            log.error("lauch executor fail >>> worker not start");
            return;
        }

        String appName = launchInfo.getAppName();
        int cpuCore = launchInfo.getCpuCore();

        String executorWorkerAddress = NetUtils.getIpPort(config.getWorkerHost(), config.getWorkerPort());
        int executorPort = getAvailableExecutorPort();
        if (executorPort > 0) {
            String executorId = launchInfo.getExecutorId();
            if (config.isAllowEmbeddedExecutor()) {
                Executor executor = new Executor(rpcEnv, appName, workerId, executorId, config.getLogPath(),
                        launchInfo.getExecutorSchedulerAddress(), executorWorkerAddress, true);
                executor.createEndpoint();

                embeddedExecutors.put(appExecutorId(appName, executorId), executor);

                log.info("lauch executor success >>> appName:{}, executorId:{}, executorAddress:{}",
                        appName, executorId, NetUtils.getIpPort(config.getWorkerHost(), executorPort));
            } else {
                //启动新jvm来启动Executor
                /**
                 * 控制jvm使用处理器数量
                 * JDK 8u191之后 可通过-XX:ActiveProcessorCount=2
                 * JDK 8u121之后 可通过 taskset -c 0-1 command param....
                 */
                int commandExecResult = 0;
                String reason = "";
                String command =
                        MessageFormat.format(
                                "java -server -XX:ActiveProcessorCount={0} -cp lib/* org.kin.scheduler.core.executor.ExecutorRunner",
                                cpuCore);
                try {
                    commandExecResult = CommandUtils.execCommand(command,
                            "", "./",
                            appName, workerId, executorId, config.getWorkerHost(), String.valueOf(executorPort),
                            config.getLogPath(), launchInfo.getExecutorSchedulerAddress(), executorWorkerAddress,
                            config.getSerialization(), Integer.toString(config.getCompressionType().getId()));
                } catch (Exception e) {
                    reason = ExceptionUtils.getExceptionDesc(e);
                }
                if (commandExecResult == 0) {
                    log.info("lauch executor success >>> appName:{}, executorId:{}, executorAddress:{}",
                            appName, executorId, NetUtils.getIpPort(config.getWorkerHost(), executorPort));
                } else {
                    log.error("lauch executor fail >>> ".concat("executor launch fail >>>>>").concat(System.lineSeparator()).concat(reason));
                }
            }
        } else {
            log.error("lauch executor fail >>> can not find available port for executor");
        }
    }

    /**
     * @return 获取可用的executor 端口
     */
    private int getAvailableExecutorPort() {
        int executorPort;
        if (config.isAllowEmbeddedExecutor()) {
            executorPort = config.getWorkerPort();
        } else {
            executorPort = config.getExecutorPort();
            while (NetUtils.isPortInRange(executorPort) && !NetUtils.isValidPort(executorPort)) {
                executorPort++;
            }
        }

        return NetUtils.isPortInRange(executorPort) ? executorPort : -1;
    }

    /**
     * scheduler 读取worker上的文件
     */
    private void readFile(MessagePostContext context, ReadFile readFile) {
        if (isStopped) {
            return;
        }
        String path = readFile.getPath();
        int fromLineNum = readFile.getFromLineNum();

        if (StringUtils.isBlank(path)) {
            context.response(TaskExecFileContent.fail(workerId, path, fromLineNum, "path is blank"));
            return;
        }

        File logFile = new File(path);
        if (!logFile.exists()) {
            context.response(TaskExecFileContent.fail(workerId, path, fromLineNum, "read file fail, file not found"));
            return;
        }

        StringBuffer logContentBuffer = new StringBuffer();
        int toLineNum = 0;
        //从指定行开始读取内容
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
            context.response(TaskExecFileContent.fail(workerId, path, fromLineNum, ExceptionUtils.getExceptionDesc(e)));
            return;
        }
        context.response(TaskExecFileContent.success(workerId, path, fromLineNum, toLineNum, logContentBuffer.toString(), fromLineNum == toLineNum));
    }

    /**
     * executor通知worker其状态变化, worker进而通知master该executor状态变化
     */
    private void executorStateChanged(ExecutorStateChanged executorStateChanged) {
        if (isStopped) {
            return;
        }
        masterRef.fireAndForget(executorStateChanged);
        if (executorStateChanged.getState().isFinished()) {
            Executor invalidExecutor =
                    embeddedExecutors.remove(
                            appExecutorId(executorStateChanged.getAppName(), executorStateChanged.getExecutorId()));
            invalidExecutor.stop();
        }
    }

    private void remoteDisconnected(KinRpcAddress rpcAddress) {
        if (isStopped) {
            return;
        }

        if (masterRef.getEndpointAddress().getRpcAddress().equals(rpcAddress)) {
            //master 断链
            registered = false;
            rpcEnv.destroyEndpointRef(masterRef);
        }
    }
}
