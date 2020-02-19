package org.kin.scheduler.core.worker;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.executor.ExecutorRunner;
import org.kin.scheduler.core.master.MasterBackend;
import org.kin.scheduler.core.master.WorkerRes;
import org.kin.scheduler.core.utils.LogUtils;
import org.kin.scheduler.core.worker.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class Worker extends AbstractService implements WorkerBackend {
    private Logger log;

    private String workerId;
    //worker配置
    private final Config config;
    //RPC服务配置
    private ServiceConfig workerServiceConfig;
    //RPC引用配置
    private ReferenceConfig<MasterBackend> masterBackendReferenceConfig;
    //master 的rpc接口
    private MasterBackend masterBackend;
    //executor 的rpc接口
    private Map<String, ExecutorContext> executors = new HashMap<>();
    //executorId counter
    //类actor执行, 所有rpc请求都是同一线程处理, 不需要用原子类
    private int executorIdCounter = 1;
    //已使用资源
    private WorkerRes res;
    //embedded executor threads
    private ThreadManager embeddedExecutorThreads;

    public Worker(String workerId, Config config) {
        super("Worker-".concat(workerId));
        this.workerId = workerId;
        this.config = config;
    }

    @Override
    public void init() {
        super.init();
        log = LogUtils.getWorkerLogger(config.getLogPath(), workerId);
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
            ExceptionUtils.log(e);
            System.exit(-1);
        }

        res = new WorkerRes(workerId);

        if (config.isAllowEmbeddedExecutor()) {
            embeddedExecutorThreads = new ThreadManager(
                    new ThreadPoolExecutor(config.getParallelism(), config.getParallelism(), 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>()));
        }
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
                log.error("worker register error >>> {}".concat(registerResult.getDesc()));
                stop();
                System.exit(-1);
            }
        } catch (Exception e) {
            log.error("worker register encounter error >>> {}", e);
            stop();
            System.exit(-1);
        }

        JvmCloseCleaner.DEFAULT().add(JvmCloseCleaner.MAX_PRIORITY, () -> stop());

        log.info("worker({}) started", workerId);
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
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
                log.error("worker unregister error >>> ", unregisterResult.getDesc());
            }
        } catch (Exception e) {
            log.error("worker unregister encounter error >>> {}", e);
        }
        stop0();
    }

    private void stop0() {
        //关闭RPC 服务
        masterBackendReferenceConfig.disable();
        workerServiceConfig.disable();

        for (ExecutorContext executorContext : executors.values()) {
            executorContext.stop();
        }
        executors.clear();

        embeddedExecutorThreads.shutdown();

        log.info("worker({}) closed", workerId);
    }

    private WorkerRegisterInfo generateWorkerRegisterInfo() {
        return new WorkerRegisterInfo(generateWorkerInfo());
    }

    private WorkerInfo generateWorkerInfo() {
        return new WorkerInfo(workerId, NetUtils.getIpPort(config.getWorkerBackendHost(), config.getWorkerBackendPort()),
                0, 0,
                res.getParallelism(), config.getParallelism());
    }

    @Override
    public ExecutorLaunchResult launchExecutor(ExecutorLaunchInfo launchInfo) {
        ExecutorLaunchResult result = launchExecutor0(launchInfo);

        //log
        if (result.isSuccess()) {
            log.info("lauch executor success >>> executorId({}), executorBackendAddress({})", result.getExecutorId(), result.getAddress());
        } else {
            log.error("lauch executor fail >>> {}", result.getDesc());
        }

        return result;
    }

    private ExecutorLaunchResult launchExecutor0(ExecutorLaunchInfo launchInfo) {
        ExecutorLaunchResult result;
        EmbeddedExecutorRunnable embeddedExecutorRunnable = null;

        //检查启动的Executor并发数是否超过上限
        if (launchInfo.getParallelism() + res.getParallelism() > config.getParallelism()) {
            return ExecutorLaunchResult.failure(String.format("launching executor's parallelism is to greater(freeParallelism=%d, needParallelism=%d)", config.getParallelism() - res.getParallelism(), launchInfo.getParallelism()));
        }

        int executorBackendPort = getAvailableExecutorBackendPort();
        if (executorBackendPort > 0) {
            String executorId = workerId.concat("-Executor").concat(String.valueOf(executorIdCounter++));

            if (config.isAllowEmbeddedExecutor()) {
                //启动内置Executor
                int executorParallelism = launchInfo.getParallelism();
                embeddedExecutorRunnable = new EmbeddedExecutorRunnable(executorId, executorBackendPort, executorParallelism);
                embeddedExecutorThreads.execute(embeddedExecutorRunnable);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {

                }
                //TODO 优化启动好executor才返回
                result = ExecutorLaunchResult.success(executorId, NetUtils.getIpPort(config.getWorkerBackendHost(), executorBackendPort));
            } else {
                //TODO 启动新jvm来启动Executor
                result = ExecutorLaunchResult.failure("not implement");
            }
        } else {
            result = ExecutorLaunchResult.failure("can not find available port for executor");
        }

        if (result.isSuccess()) {
            connectExecutor(result.getExecutorId(), result.getAddress(), embeddedExecutorRunnable);
            res.useParallelismRes(launchInfo.getParallelism());
        }

        return result;
    }

    private int getAvailableExecutorBackendPort() {
        int executorBackendPort;
        if(config.isAllowEmbeddedExecutor()){
            executorBackendPort = config.getWorkerBackendPort();
        }
        else{
            executorBackendPort = config.getExecutorBackendPort();
            while (NetUtils.isPortInRange(executorBackendPort) && !NetUtils.isValidPort(executorBackendPort)) {
                executorBackendPort++;
            }
        }

        return NetUtils.isPortInRange(executorBackendPort) ? executorBackendPort : -1;
    }

    @Override
    public void shutdownExecutor(String executorId) {
        ExecutorContext executor = executors.remove(executorId);
        if (executor != null) {
            executor.stop();
        }
    }

    private void connectExecutor(String executorId, String executorBackendAddress, EmbeddedExecutorRunnable embeddedExecutorRunnable) {
        ExecutorContext executor = new ExecutorContext(executorId, embeddedExecutorRunnable);
        executor.start(executorBackendAddress);

        executors.put(executorId, executor);
    }

    //--------------------------------------------------------------------------------------------------
    class EmbeddedExecutorRunnable implements Runnable {
        private String executorId;
        private int executorBackendPort;
        private int executorParallelism;

        private Thread curThread;

        public EmbeddedExecutorRunnable(String executorId, int executorBackendPort, int executorParallelism) {
            this.executorId = executorId;
            this.executorBackendPort = executorBackendPort;
            this.executorParallelism = executorParallelism;
        }

        @Override
        public void run() {
            curThread = Thread.currentThread();
            ExecutorRunner.runExecutor(workerId, executorId, config.getWorkerBackendHost(), executorBackendPort, executorParallelism, config.getLogPath());
        }

        public void interrupt() {
            curThread.interrupt();
        }
    }
}
