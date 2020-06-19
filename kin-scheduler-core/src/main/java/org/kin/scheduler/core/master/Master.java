package org.kin.scheduler.core.master;

import ch.qos.logback.classic.Logger;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.RpcMessageCallContext;
import org.kin.kinrpc.message.core.ThreadSafeRpcEndpoint;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.driver.ApplicationDescription;
import org.kin.scheduler.core.driver.transport.ApplicationEnd;
import org.kin.scheduler.core.driver.transport.ReadFile;
import org.kin.scheduler.core.driver.transport.RegisterApplication;
import org.kin.scheduler.core.executor.domain.ExecutorState;
import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;
import org.kin.scheduler.core.log.Loggers;
import org.kin.scheduler.core.master.domain.ApplicationContext;
import org.kin.scheduler.core.master.domain.ExecutorResource;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;
import org.kin.scheduler.core.master.transport.*;
import org.kin.scheduler.core.worker.transport.*;

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
    private final String name;
    /** 已注册的worker */
    private Map<String, WorkerContext> workers = new ConcurrentHashMap<>();
    /** 已注册的应用 */
    private Map<String, ApplicationContext> drivers = new ConcurrentHashMap<>();
    /**
     * copy on write方式更新
     * 等待资源分配的应用
     */
    private volatile List<ApplicationContext> waitingDrivers = new ArrayList<>();
    //心跳时间(秒)
    private final int heartbeatTime;
    //心跳检测间隔(秒)
    private final int heartbeatCheckInterval;
    /** 负责执行会阻塞的任务 or 调度心跳 */
    private ExecutionContext commonWorkers;
    private volatile boolean isStopped;

    public Master(RpcEnv rpcEnv, String logPath, int heartbeatTime) {
        this(DEFAULT_NAME, rpcEnv, logPath, heartbeatTime);
    }

    public Master(String name, RpcEnv rpcEnv, String logPath, int heartbeatTime) {
        super(rpcEnv);
        this.name = name;
        this.heartbeatTime = heartbeatTime;
        this.heartbeatCheckInterval = heartbeatTime + 2000;
        log = Loggers.master(logPath, name);
        commonWorkers = ExecutionContext.fix(
                SysUtils.getSuitableThreadNum(), name.concat("-common"), 2, name.concat("-common-schedule"));
    }

    //-------------------------------------------------------------------------------------------------
    public void start() {
        if (isStopped) {
            return;
        }
        rpcEnv.register(name, this);
    }

    public void stop() {
        if (isStopped) {
            return;
        }
        rpcEnv.unregister(name, this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        rpcEnv.startServer();

        JvmCloseCleaner.DEFAULT().add(JvmCloseCleaner.MAX_PRIORITY, this::stop);

        commonWorkers.scheduleAtFixedRate(() -> send2Self(CheckHeartbeatTimeout.INSTANCE),
                heartbeatCheckInterval, heartbeatCheckInterval, TimeUnit.MILLISECONDS);

        log.info("Master '{}' started", name);
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
    public void receive(RpcMessageCallContext context) {
        super.receive(context);
        Serializable message = context.getMessage();
        //master backend相关
        if (message instanceof RegisterWorker) {
            registerWorker((RegisterWorker) message);
        } else if (message instanceof UnRegisterWorker) {
            unregisterWorker(((UnRegisterWorker) message).getWorkerId());
        } else if (message instanceof WorkerHeartbeat) {
            workerHeartbeat((WorkerHeartbeat) message);
        } else if (message instanceof ExecutorStateChanged) {
            executorStateChanged((ExecutorStateChanged) message);
        } else if (message instanceof CheckHeartbeatTimeout) {
            checkHeartbeatTimeout();
        } else if (message instanceof LaunchExecutorResp) {
            launchExecutorResult((LaunchExecutorResp) message);
        }
        //driver backend相关
        else if (message instanceof RegisterApplication) {
            registerApplication(context, (RegisterApplication) message);
        } else if (message instanceof ApplicationEnd) {
            applicationEnd((ApplicationEnd) message);
        } else if (message instanceof ReadFile) {
            commonWorkers.execute(() -> readFile(context, (ReadFile) message));
        }

    }

    //------------------------------------------------------------MasterBackend-------------------------------------------------------------------

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

                log.info("worker '{}' registered", workerId);
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
        if (!isStopped && StringUtils.isNotBlank(workerId) && workers.containsKey(workerId)) {
            workers.remove(workerId);

            //广播Driver更新Executor资源
            executorStateChanged(workerId);
        }
    }


    /**
     * 定时往master发送心跳
     * 1. 移除超时worker
     * 2. 发现心跳worker还没注册, 通知其注册
     */
    private void workerHeartbeat(WorkerHeartbeat heartbeat) {
        if (!isStopped) {
            String hearbeatWorkerId = heartbeat.getWorkerId();
            WorkerContext workerContext = workers.get(hearbeatWorkerId);
            if (Objects.nonNull(workerContext)) {
                workerContext.setLastHeartbeatTime(System.currentTimeMillis());
            } else {
                //发现心跳worker还没注册, 通知其注册
                heartbeat.getWorkerRef().send(WorkerReRegister.INSTANCE);
            }
        }
    }

    /**
     * executor状态变化
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
                ExecutorResource executorResource = driver.removeExecutorResource(executorId);
                if (Objects.nonNull(executorResource)) {
                    WorkerResource workerResource = executorResource.getWorkerResource();
                    WorkerContext workerContext = workers.get(workerResource.getWorkerId());
                    if (Objects.nonNull(workerContext)) {
                        workerContext.getResource().recoverCpuCore(workerResource.getCpuCore());
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

    private void executorStateChanged(String unAvailableWorkerId) {
        for (ApplicationContext driver : drivers.values()) {
            //无用Executor
            List<String> unAvailableExecutorIds =
                    driver.getUsedExecutorResources().stream()
                            .filter(er -> er.getWorkerResource().getWorkerId().equals(unAvailableWorkerId))
                            .map(ExecutorResource::getExecutorId)
                            .collect(Collectors.toList());
            driver.ref().send(ExecutorStateUpdate.of(Collections.emptyList(), unAvailableExecutorIds));

            tryWaitingResource(driver);
        }
        scheduleResource();
    }

    //-------------------------------------------------------------driver backend 相关-------------------------------------------

    /**
     * 往master注册app
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

        ApplicationContext driver = new ApplicationContext(appDesc, registerApplication.getDriverRef());
        drivers.put(appName, driver);
        context.reply(RegisterApplicationResp.success());
        log.info("application '{}' registered", appName);
        scheduleResource(driver);
    }

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
        List<WorkerContext> registeredWorkerContexts = new ArrayList<>(workers.values());
        registeredWorkerContexts = registeredWorkerContexts.stream()
                //worker资源满足minCoresPerExecutor
                .filter(wc -> wc.getResource().getCpuCore() >= minCoresPerExecutor &&
                        //1.每个worker可以多个executor
                        //2.该worker还未分配executor
                        (!oneExecutorPerWorker || driver.containsWorkerResource(wc.getWorkerInfo().getWorkerId())))
                .collect(Collectors.toList());

        //C.根据资源分配策略获取准备要分配资源的worker
        List<WorkerContext> strategiedWorkerContexts = allocateStrategy.allocate(registeredWorkerContexts);
        if (CollectionUtils.isNonEmpty(strategiedWorkerContexts)) {
            for (WorkerContext availableWorkerContext : strategiedWorkerContexts) {
                //D.executor源分配
                try {
                    cpuCoreLeft = driver.getCpuCoreLeft();
                    if (cpuCoreLeft <= 0) {
                        //不需要额外资源了
                        break;
                    }

                    WorkerInfo availableWorkerInfo = availableWorkerContext.getWorkerInfo();
                    String availableWorkerId = availableWorkerInfo.getWorkerId();

                    //executor需要分配的cpu核心数
                    int minAllocateCpuCore = Math.min(minCoresPerExecutor, cpuCoreLeft);

                    if (minAllocateCpuCore <= 0) {
                        //资源不足
                        continue;
                    }

                    WorkerContext worker = workers.get(availableWorkerId);

                    //启动Executor
                    worker.ref().send(LaunchExecutor.of(ref(), driver.getAppDesc().getAppName(),
                            driver.ref().getEndpointAddress().getRpcAddress().address(), minAllocateCpuCore));
                } catch (Exception e) {
                    log.error("master '" + name + "' allocate executor error >>> ", e);
                }
            }
        }
        //E.如果driver资源还未分配足够, 进入等待队列继续等待足够资源
        tryWaitingResource(driver);
    }

    private void launchExecutorResult(LaunchExecutorResp launchResult) {
        String executorId = launchResult.getExecutorId();
        String appName = launchResult.getAppName();
        String workerId = launchResult.getWorkerId();
        if (launchResult.isSuccess()) {
            //启动Executor成功
            int minAllocateCpuCore = launchResult.getCpuCore();

            WorkerContext worker = workers.get(workerId);
            if (Objects.nonNull(worker)) {
                //修改worker已使用资源
                worker.getResource().useCpuCore(minAllocateCpuCore);
            }

            ApplicationContext driver = drivers.get(appName);
            if (Objects.nonNull(driver)) {
                //修改driver已使用资源
                driver.useExecutorResource(executorId, new WorkerResource(workerId, minAllocateCpuCore));
            }
        } else {
            log.warn("launchExecutor error >>>>> app '{}', worker '{}'>>>>{}", appName, workerId, launchResult.getDesc());
        }
    }

    private void scheduleResource() {
        List<ApplicationContext> waitingDrivers = new ArrayList<>(this.waitingDrivers);
        this.waitingDrivers = new ArrayList<>();
        for (ApplicationContext waitingDriver : waitingDrivers) {
            scheduleResource(waitingDriver);
        }
    }

    private void tryWaitingResource(ApplicationContext driver) {
        //资源分配不足仍然需要在队列等待有足够的资源分配
        if (driver.getCpuCoreLeft() > 0) {
            List<ApplicationContext> waitingDrivers = new ArrayList<>(this.waitingDrivers);
            waitingDrivers.add(driver);
            this.waitingDrivers = waitingDrivers;
        }
    }

    /**
     * 告诉master application完成, 释放资源
     *
     * @param applicationEnd 也就是appName
     */
    private void applicationEnd(ApplicationEnd applicationEnd) {
        String appName = applicationEnd.getAppName();
        if (!isStopped) {
            ApplicationContext driver = drivers.remove(appName);
            if (Objects.nonNull(driver)) {
                List<ApplicationContext> waitingDrivers = new ArrayList<>(this.waitingDrivers);
                waitingDrivers.remove(driver);
                this.waitingDrivers = waitingDrivers;

                //回收应用占用的资源
                for (ExecutorResource usedExecutorResource : driver.getUsedExecutorResources()) {
                    WorkerResource workerResource = usedExecutorResource.getWorkerResource();
                    WorkerContext workerContext = workers.get(workerResource.getWorkerId());
                    if (Objects.nonNull(workerContext)) {
                        workerContext.getResource().recoverCpuCore(workerResource.getCpuCore());
                    }
                }
                scheduleResource();
                log.info("applicaton '{}' shutdown", driver.getAppDesc());
            }
        }
    }

    /**
     * 从某worker上的读取文件
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

    private void checkHeartbeatTimeout() {
        try {
            long sleepTime = heartbeatCheckInterval - System.currentTimeMillis() % heartbeatCheckInterval;
            if (sleepTime > 0 && sleepTime < heartbeatCheckInterval) {
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            return;
        }

        HashSet<String> registeredWorkerIds = new HashSet<>(workers.keySet());
        for (String registeredWorkerId : registeredWorkerIds) {
            WorkerContext workerContext = workers.get(registeredWorkerId);
            if (Objects.nonNull(workerContext) &&
                    workerContext.getLastHeartbeatTime() - System.currentTimeMillis() > TimeUnit.MILLISECONDS.toMillis(heartbeatTime)) {
                //定时检测心跳超时, 并移除超时worker
                unregisterWorker(workerContext.getWorkerInfo().getWorkerId());
            }
        }
    }
}
