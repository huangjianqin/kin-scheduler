package org.kin.scheduler.core.executor;

import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.driver.SchedulerBackend;
import org.kin.scheduler.core.driver.transport.ExecutorRegisterInfo;
import org.kin.scheduler.core.driver.transport.TaskStatusChanged;
import org.kin.scheduler.core.executor.domain.ExecutorState;
import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;
import org.kin.scheduler.core.executor.transport.TaskSubmitResult;
import org.kin.scheduler.core.log.LogUtils;
import org.kin.scheduler.core.log.Loggers;
import org.kin.scheduler.core.log.TaskLoggerContext;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.TaskHandlers;
import org.kin.scheduler.core.transport.RPCResult;
import org.kin.scheduler.core.worker.ExecutorWorkerBackend;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class Executor extends AbstractService implements ExecutorBackend {
    protected final String appName;
    /** 所属的workerId */
    protected final String workerId;
    protected final String executorId;
    protected final String backendHost;
    protected final int backendPort;
    /** Executor的线程池, task执行线程池 */
    protected ExecutionContext executionContext;
    /** log路径 */
    protected final String logPath;
    /** 日志信息 */
    protected TaskLoggerContext taskLoggerContext;
    /** logger */
    protected Logger log;
    private ServiceConfig executorBackendServiceConfig;
    /** driver地址 */
    protected final String executorDriverBackendAddress;
    protected ReferenceConfig<SchedulerBackend> schedulerBackendReferenceConfig;
    /** driver引用配置 */
    protected SchedulerBackend schedulerBackend;
    /** worker地址 */
    protected final String executorWorkerBackendAddress;
    protected ReferenceConfig<ExecutorWorkerBackend> executorWorkerBackendReferenceConfig;
    /** worker引用配置 */
    protected ExecutorWorkerBackend executorWorkerBackend;
    /** 是否内嵌在worker中 */
    protected boolean isLocal;

    //--------------------------------------------------------------------
    /** 存储task执行runnable, 用于中断task执行 */
    private ConcurrentMap<String, List<TaskRunner>> taskId2TaskRunners = new ConcurrentHashMap<>();

    public Executor(String appName, String workerId, String executorId,
                    String backendHost, int backendPort, String logPath,
                    String executorDriverBackendAddress, String executorWorkerBackendAddress, boolean isLocal) {
        super(executorId);
        this.appName = appName;
        this.workerId = workerId;
        this.executorId = executorId;
        this.backendHost = backendHost;
        this.backendPort = backendPort;
        this.logPath = logPath;
        this.executorDriverBackendAddress = executorDriverBackendAddress;
        this.executorWorkerBackendAddress = executorWorkerBackendAddress;
        this.isLocal = isLocal;
    }

    @Override
    public void serviceInit() {
        super.init();
        this.executionContext = ExecutionContext.cache("executor-".concat(executorId).concat("-"));
        taskLoggerContext = new TaskLoggerContext(executorId);
        log = LogUtils.getExecutorLogger(logPath, workerId, executorId);

        executorBackendServiceConfig = Services.service(this, ExecutorBackend.class)
                .appName(getName())
                .bind(backendHost, backendPort)
                .actorLike();
        try {
            executorBackendServiceConfig.export();
        } catch (Exception e) {
            log.error("executor(" + executorId + ") init error", e);
        }

        executorWorkerBackendReferenceConfig = References.reference(ExecutorWorkerBackend.class)
                .appName(getName().concat("-ExecutorWorkerBackend"))
                .urls(executorWorkerBackendAddress);
        executorWorkerBackend = executorWorkerBackendReferenceConfig.get();

        schedulerBackendReferenceConfig = References.reference(SchedulerBackend.class)
                .appName(getName().concat("-ExecutorDriverBackend"))
                .urls(executorDriverBackendAddress);
        schedulerBackend = schedulerBackendReferenceConfig.get();

        executorWorkerBackend.executorStateChanged(ExecutorStateChanged.launching(appName, executorId));
    }

    @Override
    public void serviceStart() {
        super.start();
        taskLoggerContext.start();
        executorWorkerBackend.executorStateChanged(ExecutorStateChanged.running(appName, executorId));
        schedulerBackend.registerExecutor(new ExecutorRegisterInfo(workerId, executorId, NetUtils.getIpPort(backendHost, backendPort)));
        log.info("executor({}) started", executorId);
    }

    private TaskSubmitResult execTask0(TaskDescription taskDescription, Logger log) {
        if (this.isInState(State.STARTED)) {
            log.info("execing task({})", taskDescription);

            if (StringUtils.isBlank(taskDescription.getLogFileName())) {
                //task 默认输出目录和log文件名为taskid
                taskDescription.setLogFileName(taskDescription.getTaskId());
            }
            try {
                TaskRunner newTaskRunner = new TaskRunner(taskDescription);
                List<TaskRunner> exTaskRunners = taskId2TaskRunners.get(taskDescription.getTaskId());
                switch (taskDescription.getExecStrategy()) {
                    case SERIAL_EXECUTION:
                        break;
                    case DISCARD_LATER:
                        if (CollectionUtils.isNonEmpty(exTaskRunners)) {
                            //同一Task在同一Executor中执行
                            //保留原来执行中或者待执行的task
                            return TaskSubmitResult.failure(taskDescription.getTaskId(), "Discard Later abort task");
                        }

                        break;
                    case COVER_EARLY:
                        if (CollectionUtils.isNonEmpty(exTaskRunners)) {
                            //同一Task在同一Executor中执行
                            //中断原来执行中或者待执行的task
                            for (TaskRunner exTaskRunner : exTaskRunners) {
                                exTaskRunner.interrupt();
                            }
                        }
                        break;
                    default:
                        break;
                }

                executionContext.execute(newTaskRunner);

                //保存newTaskRunner
                exTaskRunners = taskId2TaskRunners.get(taskDescription.getTaskId());
                if (Objects.isNull(exTaskRunners)) {
                    synchronized (taskId2TaskRunners) {
                        exTaskRunners = taskId2TaskRunners.get(taskDescription.getTaskId());
                        if (Objects.isNull(exTaskRunners)) {
                            exTaskRunners = new CopyOnWriteArrayList<>();
                        }
                        exTaskRunners.add(newTaskRunner);
                        taskId2TaskRunners.put(taskDescription.getTaskId(), exTaskRunners);
                    }
                }

                String taskLogName = LogUtils.getTaskLogFileAbsoluteName(logPath, taskDescription.getJobId(), taskDescription.getTaskId(), taskDescription.getLogFileName());
                String taskOutputName = LogUtils.getTaskOutputFileAbsoluteName(logPath, taskDescription.getJobId(), taskDescription.getTaskId(), taskDescription.getLogFileName());
                return TaskSubmitResult.success(taskDescription.getTaskId(), taskLogName, taskOutputName);
            } catch (Exception e) {
                cleanFinishedTask(taskDescription);
                return TaskSubmitResult.failure(taskDescription.getTaskId(), ExceptionUtils.getExceptionDesc(e));
            }
        }

        return TaskSubmitResult.failure(taskDescription.getTaskId(), String.format("executor(%s) stopped", executorId));
    }

    @Override
    public TaskSubmitResult execTask(TaskDescription taskDescription) {
        TaskSubmitResult submitResult = execTask0(taskDescription, log);
        log.info("exec task({}) finished, resulst >>>> {}", taskDescription.getTaskId(), submitResult);
        return submitResult;
    }

    public RPCResult cancelTask0(String taskId) {
        if (isInState(State.STARTED)) {
            if (taskId2TaskRunners.containsKey(taskId)) {
                for (TaskRunner taskRunner : taskId2TaskRunners.get(taskId)) {
                    taskRunner.interrupt();
                }
                taskId2TaskRunners.remove(taskId);
                return RPCResult.success();
            }
            return RPCResult.failure(String.format("executor(%s) has not run task(%s)", executorId, taskId));
        }
        return RPCResult.failure(String.format("executor(%s) stopped", executorId));
    }

    @Override
    public RPCResult cancelTask(String taskId) {
        log.info("task({}) cancel >>>>", taskId);
        RPCResult result = cancelTask0(taskId);
        return result;
    }

    @Override
    public void destroy() {
        stop();
    }

    /**
     * 清掉已完成task的信息
     */
    private void cleanFinishedTask(TaskDescription taskDescription) {
        List<TaskRunner> exTaskRunners = taskId2TaskRunners.get(taskDescription.getTaskId());
        if (CollectionUtils.isNonEmpty(exTaskRunners)) {
            exTaskRunners.removeIf(tr -> tr.taskDescription.getTaskId().equals(taskDescription.getTaskId()));
        }
    }

    @Override
    public void serviceStop() {
        super.stop();
        executionContext.shutdown();
        taskLoggerContext.stop();

        if (isLocal) {
            try {
                executorWorkerBackend.executorStateChanged(ExecutorStateChanged.exit(appName, executorId));
            } catch (Exception e) {
                log.error("", e);
            }
        }

        executorBackendServiceConfig.disable();
        executorWorkerBackendReferenceConfig.disable();
        schedulerBackendReferenceConfig.disable();

        log.info("executor({}) closed", executorId);

        taskLoggerContext.stop();
    }

    public void executorStateChanged(ExecutorState state) {
        switch (state) {
            case KILLED:
                executorWorkerBackend.executorStateChanged(ExecutorStateChanged.kill(appName, executorId));
                break;
            case FAIL:
                executorWorkerBackend.executorStateChanged(ExecutorStateChanged.fail(appName, executorId));
                break;
            case EXIT:
                executorWorkerBackend.executorStateChanged(ExecutorStateChanged.exit(appName, executorId));
                break;
            default:
        }
    }

    //-----------------------------------------------------------------------------------------------------------------

    private class TaskRunner implements Runnable {
        private TaskDescription taskDescription;
        private Lock lock = new ReentrantLock();
        private volatile boolean isStopped;
        private Thread currentThread;

        public TaskRunner(TaskDescription taskDescription) {
            this.taskDescription = taskDescription;
        }

        @Override
        public void run() {
            //有可能未开始执行就给interrupt了
            lock.lock();
            try {
                if (isStopped) {
                    return;
                }
            } finally {
                lock.unlock();
            }

            currentThread = Thread.currentThread();
            try {
                initLogger();

                TaskStatusChanged execResult;
                Future<TaskStatusChanged> future = null;
                try {
                    if (taskDescription.getTimeout() > 0) {
                        future = executionContext.submit(this::runTask);
                        execResult = future.get(taskDescription.getTimeout(), TimeUnit.SECONDS);
                    } else {
                        execResult = runTask();
                    }
                } catch (TimeoutException e) {
                    execResult = TaskStatusChanged.fail(taskDescription.getTaskId(), taskDescription.getLogFileName(), "task execute time out");
                } catch (InterruptedException e) {
                    execResult = TaskStatusChanged.cancelled(taskDescription.getTaskId(), taskDescription.getLogFileName(), "task execute cancel");
                    Loggers.logger().info("task({}) canceled >>>>", taskDescription.getTaskId());
                } catch (Exception e) {
                    if (Objects.nonNull(future) && !future.isDone()) {
                        future.cancel(true);
                    }
                    execResult = TaskStatusChanged.fail(taskDescription.getTaskId(), taskDescription.getLogFileName(),
                            "task execute encounter error >>>>".concat(System.lineSeparator()).concat(ExceptionUtils.getExceptionDesc(e)));
                    Loggers.logger().info("task({}) execute error >>>> {}", taskDescription.getTaskId(), e);
                }

                schedulerBackend.taskStatusChange(execResult);
            } finally {
                Loggers.removeAll();
                isStopped = true;
                cleanFinishedTask(taskDescription);
            }
        }

        private void initLogger() {
            //更新上下文日志
            Loggers.updateLogger(taskLoggerContext.getTaskLogger(logPath, taskDescription.getJobId(), taskDescription.getTaskId(), taskDescription.getLogFileName()));
            Loggers.updateTaskOutputFileName(LogUtils.getTaskOutputFileAbsoluteName(logPath, taskDescription.getJobId(), taskDescription.getTaskId(), taskDescription.getLogFileName()));
        }

        private TaskStatusChanged runTask() throws Exception {
            //获取task handler
            TaskHandler taskHandler = TaskHandlers.getTaskHandler(taskDescription);
            Preconditions.checkNotNull(taskHandler, "task handler is null");

            initLogger();
            return TaskStatusChanged.finished(
                    taskDescription.getTaskId(),
                    taskDescription.getLogFileName(),
                    taskDescription.getExecStrategy().getDesc()
                            .concat("run task >>>> ")
                            .concat(taskDescription.toString()),
                    taskHandler.exec(taskDescription));
        }

        public void interrupt() {
            lock.lock();
            try {
                if (!isStopped) {
                    currentThread.interrupt();
                    isStopped = true;
                }
            } finally {
                lock.unlock();
            }

        }
    }
}
