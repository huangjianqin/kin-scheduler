package org.kin.scheduler.core.executor;

import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.driver.ExecutorDriverBackend;
import org.kin.scheduler.core.driver.transport.TaskExecResult;
import org.kin.scheduler.core.executor.domain.ExecutorState;
import org.kin.scheduler.core.executor.log.LogContext;
import org.kin.scheduler.core.executor.log.TaskExecLog;
import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;
import org.kin.scheduler.core.executor.transport.TaskSubmitResult;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.TaskHandlers;
import org.kin.scheduler.core.task.log.TaskLoggers;
import org.kin.scheduler.core.transport.RPCResult;
import org.kin.scheduler.core.utils.LogUtils;
import org.kin.scheduler.core.worker.ExecutorWorkerBackend;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
    protected LogContext logContext;
    /** logger */
    protected Logger log;
    private ServiceConfig executorBackendServiceConfig;
    /** driver地址 */
    protected final String executorDriverBackendAddress;
    protected ReferenceConfig<ExecutorDriverBackend> executorDriverBackendReferenceConfig;
    /** driver引用配置 */
    protected ExecutorDriverBackend executorDriverBackend;
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
    public void init() {
        super.init();
        this.executionContext = ExecutionContext.cache("executor-".concat(executorId).concat("-"));
        logContext = new LogContext(executorId);
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

        executorDriverBackendReferenceConfig = References.reference(ExecutorDriverBackend.class)
                .appName(getName().concat("-ExecutorDriverBackend"))
                .urls(executorDriverBackendAddress);
        executorDriverBackend = executorDriverBackendReferenceConfig.get();

        executorWorkerBackend.executorStateChanged(ExecutorStateChanged.launching(appName, executorId));
    }

    @Override
    public void start() {
        super.start();
        executorWorkerBackend.executorStateChanged(ExecutorStateChanged.running(appName, executorId));
        log.info("executor({}) started", executorId);
    }

    private TaskSubmitResult execTask0(Task task, Logger log) {
        if (this.isInState(State.STARTED)) {
            log.debug("execing task({})", task);
            try {
                TaskRunner newTaskRunner = new TaskRunner(task);
                List<TaskRunner> exTaskRunners = taskId2TaskRunners.get(task.getTaskId());
                switch (task.getExecStrategy()) {
                    case SERIAL_EXECUTION:
                        break;
                    case DISCARD_LATER:
                        if (CollectionUtils.isNonEmpty(exTaskRunners)) {
                            //同一Task在同一Executor中执行
                            //保留原来执行中或者待执行的task
                            return TaskSubmitResult.failure(task.getTaskId(), "Discard Later abort task");
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
                exTaskRunners = taskId2TaskRunners.get(task.getTaskId());
                if (Objects.isNull(exTaskRunners)) {
                    synchronized (taskId2TaskRunners) {
                        exTaskRunners = taskId2TaskRunners.get(task.getTaskId());
                        if (Objects.isNull(exTaskRunners)) {
                            exTaskRunners = new CopyOnWriteArrayList<>();
                        }
                        exTaskRunners.add(newTaskRunner);
                        taskId2TaskRunners.put(task.getTaskId(), exTaskRunners);
                    }
                }

                return TaskSubmitResult.success(task.getTaskId(), LogUtils.getTaskLogFileAbsoluteName(logPath, task.getJobId(), task.getTaskId(), task.getLogFileName()));
            } catch (Exception e) {
                cleanFinishedTask(task);
                return TaskSubmitResult.failure(task.getTaskId(), ExceptionUtils.getExceptionDesc(e));
            }
        }

        return TaskSubmitResult.failure(task.getTaskId(), String.format("executor(%s) stopped", executorId));
    }

    @Override
    public TaskSubmitResult execTask(Task task) {
        TaskSubmitResult submitResult = execTask0(task, log);
        log.debug("exec task({}) finished, resulst >>>> {}", task.getTaskId(), submitResult);
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
        log.debug("task({}) cancel >>>>", taskId);
        RPCResult result = cancelTask0(taskId);
        return result;
    }

    @Override
    public TaskExecLog readLog(String logPath, int fromLineNum) {
        if (StringUtils.isBlank(logPath)) {
            return new TaskExecLog(fromLineNum, 0, "readLog fail, logFile not found", true);
        }

        File logFile = new File(logPath);
        if (!logFile.exists()) {
            return new TaskExecLog(fromLineNum, 0, "readLog fail, logFile not found", true);
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
        }
        return new TaskExecLog(fromLineNum, toLineNum, logContentBuffer.toString(), fromLineNum == toLineNum);
    }

    @Override
    public void destroy() {
        stop();
    }

    /**
     * 清掉已完成task的信息
     */
    private void cleanFinishedTask(Task task) {
        List<TaskRunner> exTaskRunners = taskId2TaskRunners.get(task.getTaskId());
        exTaskRunners.removeIf(tr -> tr.task.getTaskId().equals(task.getTaskId()));
    }

    @Override
    public void stop() {
        super.stop();
        executionContext.shutdown();
        logContext.stop();

        if (isLocal) {
            executorWorkerBackend.executorStateChanged(ExecutorStateChanged.exit(appName, executorId));
        }

        executorBackendServiceConfig.disable();
        executorWorkerBackendReferenceConfig.disable();
        executorDriverBackendReferenceConfig.disable();

        log.info("executor({}) closed", executorId);
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
        private Task task;
        private Lock lock = new ReentrantLock();
        private volatile boolean isStopped;
        private Thread currentThread;

        public TaskRunner(Task task) {
            this.task = task;
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
                TaskExecResult execResult = null;
                if (task.getTimeout() > 0) {
                    Future<TaskExecResult> future = null;
                    try {
                        future = executionContext.submit(this::runTask);
                        execResult = future.get(task.getTimeout(), TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        execResult = TaskExecResult.failure(task.getTaskId(), task.getLogFileName(), "task execute time out");
                    } catch (Exception e) {
                        if (Objects.nonNull(future) && !future.isDone()) {
                            future.cancel(true);
                        }
                        if (e instanceof InterruptedException) {
                            execResult = TaskExecResult.failure(task.getTaskId(), task.getLogFileName(), "task execute cancel");
                            TaskLoggers.logger().debug("task({}) canceled >>>>", task.getTaskId());
                        } else {
                            execResult = TaskExecResult.failure(task.getTaskId(), task.getLogFileName(), "task execute encounter error >>>>".concat(ExceptionUtils.getExceptionDesc(e)));
                        }
                    }
                } else if (task.getTimeout() == 0) {
                    execResult = runTask();
                }

                executorDriverBackend.taskFinish(execResult);
            } finally {
                TaskLoggers.logger().detachAndStopAllAppenders();
                TaskLoggers.removeAll();
                isStopped = true;
                cleanFinishedTask(task);
            }
        }

        private TaskExecResult runTask() {
            //获取task handler
            TaskHandler taskHandler = TaskHandlers.getTaskHandler(task);
            Preconditions.checkNotNull(taskHandler, "task handler is null");

            //更新上下文日志
            TaskLoggers.updateLogger(logContext.getTaskLogger(logPath, task.getJobId(), task.getTaskId(), task.getLogFileName()));
            TaskLoggers.updateLoggerFile(LogUtils.getTaskLogFileAbsoluteName(logPath, task.getJobId(), task.getTaskId(), task.getLogFileName()));
            TaskExecResult execResult = null;
            try {
                execResult = TaskExecResult.success(
                        task.getTaskId(),
                        task.getLogFileName(),
                        task.getExecStrategy().getDesc()
                                .concat("run task >>>> ")
                                .concat(task.toString()),
                        taskHandler.exec(task));
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    TaskLoggers.logger().debug("task({}) canceled >>>>", task.getTaskId());
                } else {
                    TaskExecResult.failure(task.getTaskId(), task.getLogFileName(), "task execute encounter error >>>>".concat(ExceptionUtils.getExceptionDesc(e)));
                }
            }
            return execResult;
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
