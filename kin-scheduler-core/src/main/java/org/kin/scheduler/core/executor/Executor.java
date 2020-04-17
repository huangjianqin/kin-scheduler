package org.kin.scheduler.core.executor;

import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.*;
import org.kin.scheduler.core.domain.RPCResult;
import org.kin.scheduler.core.driver.ExecutorDriverBackend;
import org.kin.scheduler.core.executor.domain.TaskExecLog;
import org.kin.scheduler.core.executor.domain.TaskExecResult;
import org.kin.scheduler.core.executor.domain.TaskSubmitResult;
import org.kin.scheduler.core.log.LogContext;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.task.TaskLoggers;
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.TaskHandlers;
import org.kin.scheduler.core.task.handler.impl.ScriptHandler;
import org.kin.scheduler.core.utils.LogUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class Executor extends AbstractService implements ExecutorBackend {
    /** 所属的workerId */
    protected String workerId;
    protected String executorId;
    /** Executor的线程池, task执行线程池 */
    protected ExecutionContext executionContext;
    /** log路径 */
    protected String logPath;
    /** 日志信息 */
    protected LogContext logContext;
    /** logger */
    protected Logger log;
    /** driver服务配置 */
    protected ExecutorDriverBackend executorDriverBackend;

    //--------------------------------------------------------------------
    /** 存储task执行runnable, 用于中断task执行 */
    private ConcurrentMap<String, List<TaskRunner>> taskId2TaskRunners = new ConcurrentHashMap<>();
    /** 存储执行过的jobId, 用于shutdown executor时, 清理job占用的脚本资源 */
    private Set<String> execedJobIds = new HashSet<>();

    public Executor(String workerId, String executorId) {
        this(workerId, executorId, LogUtils.BASE_PATH);
    }

    public Executor(String workerId, String executorId, String logPath) {
        super(executorId);
        this.workerId = workerId;
        this.executorId = executorId;
        this.logPath = logPath;
    }

    public Executor(String workerId, String executorId, String logPath, ExecutorDriverBackend executorDriverBackend) {
        super(executorId);
        this.workerId = workerId;
        this.executorId = executorId;
        this.logPath = logPath;
        this.executorDriverBackend = executorDriverBackend;
    }

    @Override
    public void init() {
        super.init();
        this.executionContext = ExecutionContext.fix(SysUtils.CPU_NUM, "executor-".concat(executorId).concat("-"));
        logContext = new LogContext(executorId);
        log = LogUtils.getExecutorLogger(logPath, workerId, executorId);
    }

    @Override
    public void start() {
        super.start();
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
        exTaskRunners.remove(task);
    }

    @Override
    public void stop() {
        super.stop();
        executionContext.shutdown();
        logContext.stop();
        //清理job占用的脚本资源
        for (String execedJobId : execedJobIds) {
            File file = new File(ScriptHandler.getOrCreateRealRunEnvPath(execedJobId));
            if (file.exists()) {
                FileUtils.delete(file);
            }
        }
        log.info("executor({}) closed", executorId);
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
                        future = executionContext.submit(() -> runTask());
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

            execedJobIds.add(task.getJobId());
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
