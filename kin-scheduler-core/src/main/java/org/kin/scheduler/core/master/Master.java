package org.kin.scheduler.core.master;

import ch.qos.logback.classic.Logger;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.RpcMessageCallContext;
import org.kin.kinrpc.message.core.ThreadSafeRpcEndpoint;
import org.kin.kinrpc.message.core.message.ClientDisconnected;
import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.driver.ApplicationDescription;
import org.kin.scheduler.core.driver.transport.ApplicationEnd;
import org.kin.scheduler.core.driver.transport.ReadFile;
import org.kin.scheduler.core.driver.transport.RegisterApplication;
import org.kin.scheduler.core.executor.domain.ExecutorState;
import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;
import org.kin.scheduler.core.log.Loggers;
import org.kin.scheduler.core.master.domain.ApplicationContext;
import org.kin.scheduler.core.master.domain.ExecutorDesc;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;
import org.kin.scheduler.core.master.transport.*;
import org.kin.scheduler.core.worker.domain.WorkerInfo;
import org.kin.scheduler.core.worker.transport.RegisterWorker;
import org.kin.scheduler.core.worker.transport.TaskExecFileContent;
import org.kin.scheduler.core.worker.transport.WorkerHeartbeat;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class Master extends ThreadSafeRpcEndpoint {
    private Logger log;
    public static final String DEFAULT_NAME = "Master";

    //-------------------------------------------------------------------------------------------------
    /** master name */
    private final String name;
    /** 配置 */
    private final Config config;
    /** 已注册的worker */
    private Map<String, WorkerContext> workers = new ConcurrentHashMap<>();
    /** 已注册的应用 */
    private Map<String, ApplicationContext> drivers = new ConcurrentHashMap<>();
    /**
     * copy on write方式更新
     * 等待资源分配的应用
     */
    private volatile List<ApplicationContext> waitingDrivers = new ArrayList<>();
    /** 负责执行会阻塞的任务 or 调度心跳 */
    private ExecutionContext commonWorkers;
    private volatile boolean isStopped;

    public Master(RpcEnv rpcEnv, Config config) {
        this(DEFAULT_NAME, rpcEnv, config);
    }

    public Master(String name, RpcEnv rpcEnv, Config config) {
        super(rpcEnv);
        this.name = name;
        this.config = config;
        log = Loggers.master(config.getLogPath(), name);
        commonWorkers = ExecutionContext.elastic(1, SysUtils.getSuitableThreadNum(), name.concat("-common"), 2, name.concat("-common-schedule"));
    }

    //-------------------------------------------------------------------------------------------------
    public void createEndpoint() {
        if (isStopped) {
            return;
        }
        rpcEnv.register(name, this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //定时心跳超时检查
        commonWorkers.scheduleAtFixedRate(() -> send2Self(CheckHeartbeatTimeout.INSTANCE),
                config.getHeartbeatCheckInterval(), config.getHeartbeatCheckInterval(), TimeUnit.MILLISECONDS);

        log.info("Master '{}' started on {}", name, rpcEnv.address().address());
    }

    @Override
    protected void onStop() {
        super.onStop();

        isStopped = true;
        workers.clear();
        rpcEnv.stop();
        log.info("Master '{}' stopped", name);
    }

    @Override
    public void onReceiveMessage(RpcMessageCallContext context) {
        Serializable message = context.getMessage();
        //master endpoint相关
        if (message instanceof RegisterWorker) {
            registerWorker((RegisterWorker) message);
        } else if (message instanceof WorkerHeartbeat) {
            workerHeartbeat((WorkerHeartbeat) message);
        } else if (message instanceof ExecutorStateChanged) {
            executorStateChanged((ExecutorStateChanged) message);
        } else if (message instanceof CheckHeartbeatTimeout) {
            checkHeartbeatTimeout();
        }
        //scheduler endpoint相关
        else if (message instanceof RegisterApplication) {
            registerApplication(context, (RegisterApplication) message);
        } else if (message instanceof ApplicationEnd) {
            applicationEnd(((ApplicationEnd) message).getAppName());
        } else if (message instanceof ReadFile) {
            commonWorkers.execute(() -> readFile(context, (ReadFile) message));
        } else if (message instanceof ClientDisconnected) {
            remoteDisconnected(((ClientDisconnected) message).getRpcAddress());
        }
    }

    //------------------------------------------------------------Master endpoint-------------------------------------------------------------------

    /**
     * 注册worker, 成功注册的worker才是有效的worker
     *
     * @param registerWorker worker信息
     */
    private void registerWorker(RegisterWorker registerWorker) {
        WorkerInfo workerInfo = registerWorker.getWorkerInfo();

        if (Objects.nonNull(workerInfo)) {
            RpcEndpointRef workerRef = registerWorker.getWorkerRef();
            if (isStopped) {
                workerRef.send(RegisterWorkerResp.failure("master not started"));
                return;
            }

            String workerId = workerInfo.getWorkerId();
            WorkerContext worker = workers.get(workerId);
            if (Objects.isNull(worker)) {
                worker = new WorkerContext(workerInfo, workerRef);
                workers.put(workerId, worker);

                log.info("worker '{}' registered >>>>> address:{}, cpu:{}, memory:{}",
                        workerId, workerRef.getEndpointAddress().getRpcAddress().address(),
                        workerInfo.getMaxCpuCore(), workerInfo.getMaxMemory());
                workerRef.send(RegisterWorkerResp.success());
                //调度资源
                scheduleResource();
            } else {
                workerRef.send(RegisterWorkerResp.failure(String.format("worker(workerId=%s) has registered", workerId)));
            }
        } else {
            log.error("worker(workerId=null) register info error");
        }
    }

    /**
     * 注销worker
     *
     * @param workerId workerId
     */
    private void unregisterWorker(String workerId) {
        if (!isStopped && StringUtils.isNotBlank(workerId)) {
            WorkerContext worker = workers.remove(workerId);
            if (Objects.isNull(worker)) {
                return;
            }

            workerStateChanged(worker);
            log.info("worker '{}' unregistered", workerId);
        }
    }


    /**
     * worker定时往master发送心跳
     * 1. 移除超时worker
     * 2. 发现心跳worker还没注册, 通知其注册
     */
    private void workerHeartbeat(WorkerHeartbeat heartbeat) {
        if (!isStopped) {
            String hearbeatWorkerId = heartbeat.getWorkerId();
            WorkerContext worker = workers.get(hearbeatWorkerId);
            if (Objects.nonNull(worker)) {
                worker.setLastHeartbeatTime(System.currentTimeMillis());
            } else {
                //发现心跳worker还没注册, 通知其注册
                heartbeat.getWorkerRef().send(WorkerReRegister.INSTANCE);
            }
        }
    }

    /**
     * worker executor状态变化
     *
     * @param executorStateChanged executor状态信息
     */
    private void executorStateChanged(ExecutorStateChanged executorStateChanged) {
        if (isStopped) {
            return;
        }

        String appName = executorStateChanged.getAppName();
        String executorId = executorStateChanged.getExecutorId();
        ApplicationContext driver = drivers.get(appName);
        if (Objects.nonNull(driver) && driver.containsExecutorResource(executorId)) {
            ExecutorState executorState = executorStateChanged.getState();
            log.info("app '{}''s executor '{}' state changed, now state is {}", appName, executorId, executorState);
            if (!executorState.isFinished()) {
                if (ExecutorState.RUNNING.equals(executorState)) {
                    //已在启动executor时预占用资源
                    driver.ref().send(ExecutorStateUpdate.of(Collections.singletonList(executorId), Collections.emptyList()));
                }
                //TODO 启动状态暂时不处理
            } else {
                ExecutorDesc executorDesc = driver.removeExecutorResource(executorId);
                if (Objects.nonNull(executorDesc)) {
                    WorkerContext worker = workers.get(executorDesc.getWorker().getWorkerInfo().getWorkerId());
                    if (Objects.nonNull(worker)) {
                        worker.recoverCpuCore(executorDesc.getUsedCpuCore());
                        worker.recoverMemory(executorDesc.getUsedMemory());
                    }
                    try {
                        driver.ref().send(ExecutorStateUpdate.of(Collections.emptyList(), Collections.singletonList(executorId)));
                    } catch (Exception e) {
                        log.error("", e);
                    }
                    tryWaitingResource(driver);
                    scheduleResource();
                }
            }
        }
    }

    /**
     * 移除无效worker相关executor资源占用
     */
    private void workerStateChanged(WorkerContext worker) {
        if (!worker.getWorkerInfo().isAllowEmbeddedExecutor()) {
            return;
        }
        boolean needSchedule = false;
        //内嵌executor的worker才需要处理, 因为executor与worker同生共死
        for (ApplicationContext driver : drivers.values()) {
            //无用Executor ids
            List<String> unavailableExecutorIds =
                    driver.workerUnavailable(worker.getWorkerInfo().getWorkerId());
            if (CollectionUtils.isNonEmpty(unavailableExecutorIds)) {
                driver.ref().send(ExecutorStateUpdate.of(Collections.emptyList(), unavailableExecutorIds));
                tryWaitingResource(driver);
                needSchedule = true;
            }
        }
        if (needSchedule) {
            scheduleResource();
        }
    }

    //-------------------------------------------------------------driver endpoint 相关-------------------------------------------

    /**
     * scheduler往master注册app
     *
     * @param registerApplication 请求
     */
    private void registerApplication(RpcMessageCallContext context, RegisterApplication registerApplication) {
        if (isStopped) {
            context.reply(RegisterApplicationResp.failure("master not started"));
            return;
        }

        ApplicationDescription appDesc = registerApplication.getAppDesc();

        String appName = appDesc.getAppName();
        if (drivers.containsKey(appName)) {
            context.reply(RegisterApplicationResp.failure(String.format("application '%s' has registered", appName)));
            return;
        }

        if (Objects.isNull(registerApplication.getAppDesc().getAllocateStrategy())) {
            context.reply(RegisterApplicationResp.failure("unknown allocate strategy type"));
            return;
        }

        ApplicationContext driver = new ApplicationContext(appDesc, registerApplication.getSchedulerRef());
        drivers.put(appName, driver);
        context.reply(RegisterApplicationResp.success());
        log.info("application '{}' registered", appName);
        scheduleResource(driver);
    }

    /**
     * application分配资源
     */
    private void scheduleResource(ApplicationContext driver) {
        ApplicationDescription appDesc = driver.getAppDesc();
        AllocateStrategy allocateStrategy = appDesc.getAllocateStrategy();
        //寻找可用executor
        //A.判断driver是否可以进行资源分配
        boolean oneExecutorPerWorker = appDesc.isOneExecutorPerWorker();
        int minCoresPerExecutor = appDesc.getMinCoresPerExecutor();

        int cpuCoreLeft = driver.getCpuCoreLeft();
        if (cpuCoreLeft <= 0 || cpuCoreLeft < minCoresPerExecutor) {
            //1.不需要额外资源了
            //2.资源不满足每个Executor最低要求
            return;
        }

        //B.寻找可以分配资源的worker
        List<WorkerContext> registeredWorkers = new ArrayList<>(workers.values());
        registeredWorkers = registeredWorkers.stream()
                //worker资源满足minCoresPerExecutor
                //TODO 考虑占用内存是否足够
                .filter(wc -> wc.isAlive() &&
                        wc.hasEnoughCpuCore(minCoresPerExecutor) &&
                        //1.每个worker可以多个executor
                        //2.该worker还未分配executor
                        (!oneExecutorPerWorker || !driver.containsWorkerResource(wc.getWorkerInfo().getWorkerId())))
                .collect(Collectors.toList());

        //C.根据资源分配策略获取准备要分配资源的worker
        List<WorkerContext> strategiedWorkers = allocateStrategy.allocate(registeredWorkers);
        if (CollectionUtils.isNonEmpty(strategiedWorkers)) {
            for (WorkerContext availableWorker : strategiedWorkers) {
                //D.executor源分配
                try {
                    cpuCoreLeft = driver.getCpuCoreLeft();
                    if (cpuCoreLeft <= 0) {
                        //不需要额外资源了
                        break;
                    }

                    WorkerInfo availableWorkerInfo = availableWorker.getWorkerInfo();
                    String availableWorkerId = availableWorkerInfo.getWorkerId();

                    //executor需要分配的cpu核心数
                    int minAllocateCpuCore = Math.min(minCoresPerExecutor, cpuCoreLeft);

                    if (minAllocateCpuCore <= 0) {
                        //资源不足
                        continue;
                    }

                    WorkerContext worker = workers.get(availableWorkerId);

                    //修改worker已使用资源
                    worker.useCpuCore(minAllocateCpuCore);
                    worker.useMemory(0);

                    //修改driver已使用资源
                    String executorId = driver.useExecutorResource(worker, minAllocateCpuCore);

                    //启动Executor
                    worker.ref().send(LaunchExecutor.of(ref(), driver.getAppDesc().getAppName(),
                            driver.ref().getEndpointAddress().getRpcAddress().address(), executorId, minAllocateCpuCore));
                } catch (Exception e) {
                    log.error("master '" + name + "' allocate executor error >>> ", e);
                }
            }
        }
        //E.如果driver资源还未分配足够, 进入等待队列继续等待足够资源
        tryWaitingResource(driver);
    }

    /**
     * 给资源不足的scheduler分配资源
     */
    private void scheduleResource() {
        List<ApplicationContext> waitingDrivers = new ArrayList<>(this.waitingDrivers);
        this.waitingDrivers = new ArrayList<>();
        for (ApplicationContext waitingDriver : waitingDrivers) {
            scheduleResource(waitingDriver);
        }
    }

    /**
     * 判断是否资源仍然不足, 则继续等待
     */
    private void tryWaitingResource(ApplicationContext driver) {
        //资源分配不足仍然需要在队列等待有足够的资源分配
        if (driver.getCpuCoreLeft() > 0) {
            List<ApplicationContext> waitingDrivers = new ArrayList<>(this.waitingDrivers);
            waitingDrivers.add(driver);
            this.waitingDrivers = waitingDrivers;
        }
    }

    /**
     * 通知master application完成, 释放资源
     *
     * @param appName
     */
    private void applicationEnd(String appName) {
        if (!isStopped) {
            ApplicationContext driver = drivers.remove(appName);
            if (Objects.nonNull(driver)) {
                List<ApplicationContext> waitingDrivers = new ArrayList<>(this.waitingDrivers);
                waitingDrivers.remove(driver);
                this.waitingDrivers = waitingDrivers;

                //回收应用占用的资源
                for (ExecutorDesc executorDesc : driver.getExecutorDescs()) {
                    executorDesc.releaseResources();
                }
                scheduleResource();
                log.info("applicaton '{}' shutdown", driver.getAppDesc());
            }
        }
    }

    /**
     * driver请求在某worker上的读取文件
     *
     * @param readFile 读取文件所需参数, 也就是所在worker的唯一Id, 路径, 开始行数
     */
    private void readFile(RpcMessageCallContext context, ReadFile readFile) {
        String workerId = readFile.getWorkerId();
        String path = readFile.getPath();
        int fromLineNum = readFile.getFromLineNum();
        WorkerContext worker = workers.get(workerId);
        if (Objects.nonNull(worker)) {
            try {
                context.reply(worker.ref().ask(readFile).get());
            } catch (Exception e) {
                log.error("", e);
            }
        }
        context.reply(TaskExecFileContent.fail(workerId, path, fromLineNum, String.format("unknow worker(workerId='%s')", workerId)));
    }

    //-------------------------------------------------------------内部-------------------------------------------
    private void remoteDisconnected(KinRpcAddress rpcAddress) {
        List<WorkerContext> disconnectedWorker = workers.values().stream()
                .filter(wc -> wc.ref().getEndpointAddress().getRpcAddress().equals(rpcAddress))
                .collect(Collectors.toList());
        for (WorkerContext workerContext : disconnectedWorker) {
            unregisterWorker(workerContext.getWorkerInfo().getWorkerId());
        }


        List<ApplicationContext> disconnectedApplications = drivers.values().stream()
                .filter(ac -> ac.ref().getEndpointAddress().getRpcAddress().equals(rpcAddress))
                .collect(Collectors.toList());
        for (ApplicationContext disconnectedApplication : disconnectedApplications) {
            applicationEnd(disconnectedApplication.getAppDesc().getAppName());
        }
    }

    /**
     * master定时检查心跳超时
     */
    private void checkHeartbeatTimeout() {
        try {
            int heartbeatCheckInterval = config.getHeartbeatCheckInterval();
            long sleepTime = heartbeatCheckInterval - System.currentTimeMillis() % heartbeatCheckInterval;
            if (sleepTime > 0 && sleepTime < heartbeatCheckInterval) {
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            return;
        }

        HashSet<String> registeredWorkerIds = new HashSet<>(workers.keySet());
        for (String registeredWorkerId : registeredWorkerIds) {
            WorkerContext worker = workers.get(registeredWorkerId);
            if (Objects.isNull(worker)) {
                continue;
            }

            //失联时间
            long lossTime = worker.getLastHeartbeatTime() - System.currentTimeMillis();
            if (lossTime > config.getHeartbeatTime()) {
                if (worker.isAlive()) {
                    //先判断为无效, 超过一定阈值后才认为彻底无效, 并移除相关数据
                    worker.dead();
                    workerStateChanged(worker);
                } else {
                    if (lossTime >= TimeUnit.MINUTES.toMillis(1)) {
                        //超过1分钟
                        //超过一定阈值才彻底放弃
                        //定时检测心跳超时, 并移除超时worker
                        unregisterWorker(worker.getWorkerInfo().getWorkerId());
                    }
                }
            } else {
                if (worker.isDead()) {
                    //恢复心跳, 复活
                    worker.alive();
                    if (worker.hasResources()) {
                        scheduleResource();
                    }
                }
            }
        }
    }
}
