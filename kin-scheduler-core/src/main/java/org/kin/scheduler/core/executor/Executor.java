package org.kin.scheduler.core.executor;

import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.kin.framework.concurrent.PartitionTaskExecutor;
import org.kin.framework.concurrent.impl.EfficientHashPartitioner;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.domain.RPCResult;
import org.kin.scheduler.core.executor.domain.TaskExecResult;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.task.TaskLoggers;
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.TaskHandlers;
import org.kin.scheduler.core.utils.LogUtils;

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

    //所属的workerId
    private String workerId;

    private String executorId;
    //Executor暴露给worker的host
    private String backendHost;
    //Executor暴露给worker的端口
    private int backendPort;
    //Executor的并行数
    private int parallelism;
    //Executor的线程池, task执行线程池
    private PartitionTaskExecutor<String> threads;
    //rpc服务配置
    private ServiceConfig serviceConfig;
    //log路径
    private String logBasePath;
    //日志信息
    private LogContext logContext;
    //executor log
    private Logger log;

    //--------------------------------------------------------------------
    private ConcurrentMap<String, List<TaskRunner>> taskId2TaskRunners = new ConcurrentHashMap<>();

    public Executor(String workerId, String executorId, String backendHost, int backendPort) {
        this(workerId, executorId, backendHost, backendPort, SysUtils.CPU_NUM, LogUtils.BASE_PATH);
    }

    public Executor(String workerId, String executorId, String backendHost, int backendPort, int parallelism, String logBasePath) {
        super(executorId);
        this.workerId = workerId;
        this.executorId = executorId;
        this.backendHost = backendHost;
        this.backendPort = backendPort;
        this.parallelism = parallelism;
        this.logBasePath = logBasePath;
    }

    @Override
    public void init() {
        super.init();
        this.threads = new PartitionTaskExecutor<String>(parallelism, EfficientHashPartitioner.INSTANCE);
        logContext = new LogContext(executorId);
        log = LogUtils.getWorkerLogger(logBasePath, workerId);

        try {
            serviceConfig = Services.service(this, ExecutorBackend.class)
                    .appName(getName())
                    .bind(backendHost, backendPort);
            serviceConfig.export();
        } catch (Exception e) {
            ExceptionUtils.log(e);
            //TODO
            System.exit(-1);
        }
    }

    @Override
    public void start() {
        super.start();
        log.info("executor({}) started", executorId);
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }

    private TaskExecResult execTask0(Task task, Logger log) {
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
                            return TaskExecResult.failure("Discard Later abort task");
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

                Future<TaskExecResult> future = threads.execute(task.getTaskId(), newTaskRunner);

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

                TaskExecResult execResult;
                if (task.getTimeout() > 0) {
                    try {
                        execResult = future.get(task.getTimeout(), TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        cleanFinishedTask(task);
                        return TaskExecResult.failure("task execute time out");
                    }
                } else if (task.getTimeout() == 0) {
                    execResult = future.get();
                } else {
                    execResult = null;
                }

                return execResult;
            } catch (Exception e) {
                cleanFinishedTask(task);
                return TaskExecResult.failure(ExceptionUtils.getExceptionDesc(e));
            }
        }

        return TaskExecResult.failureWithRetry(String.format("executor(%s) stopped", executorId));
    }

    @Override
    public TaskExecResult execTask(Task task) {
        Logger log = logContext.getJobLogger(logBasePath, task.getJobId());
        TaskExecResult execResult = execTask0(task, log);
        log.debug("exec task({}) finished, resulst >>>> {}", task.getTaskId(), execResult);
        return execResult;
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
        return TaskExecResult.failureWithRetry(String.format("executor(%s) stopped", executorId));
    }

    @Override
    public RPCResult cancelTask(String jobId, String taskId) {
        Logger log = logContext.getJobLogger(logBasePath, jobId);
        RPCResult result = cancelTask0(taskId);
        log.debug("task({}) cancel result >>>>", result);
        return result;
    }

    @Override
    public void destroy() {
        close();
        System.exit(0);
    }

    /**
     * 清掉已完成task的信息
     */
    private void cleanFinishedTask(Task task) {
        List<TaskRunner> exTaskRunners = taskId2TaskRunners.get(task.getTaskId());
        exTaskRunners.remove(task);
    }

    @Override
    public void close() {
        super.close();
        if (Objects.nonNull(serviceConfig)) {
            serviceConfig.disable();
        }
        threads.shutdown();
        logContext.stop();
        log.info("executor({}) closed", executorId);
    }

    //-----------------------------------------------------------------------------------------------------------------
    private class TaskRunner implements Callable<TaskExecResult> {
        private Task task;
        private Lock lock = new ReentrantLock();
        private volatile boolean isStopped;
        private Thread currentThread;

        public TaskRunner(Task task) {
            this.task = task;
        }

        @Override
        public TaskExecResult call() {
            //有可能未开始执行就给interrupt了
            lock.lock();
            try {
                if (isStopped) {
                    return null;
                }
            } finally {
                lock.unlock();
            }

            currentThread = Thread.currentThread();
            try {
                //获取task handler
                TaskHandler taskHandler = TaskHandlers.getTaskHandler(task);
                Preconditions.checkNotNull(taskHandler, "task handler is null");
                if (taskHandler != null) {
                    //更新上下文日志
                    TaskLoggers.updateLogger(log);
                    TaskLoggers.updateLoggerFile(logContext.getJobLogFile(logBasePath, task.getJobId()));
                    return TaskExecResult.success(
                            task.getExecStrategy().getDesc()
                                    .concat("run task >>>> ")
                                    .concat(task.toString()),
                            taskHandler.exec(task));
                }
            } catch (Exception e) {
                return TaskExecResult.failure("task execute encounter error >>>>".concat(ExceptionUtils.getExceptionDesc(e)));
            } finally {
                isStopped = true;
                cleanFinishedTask(task);
            }

            return TaskExecResult.failure("task execute encounter unknown error");
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
