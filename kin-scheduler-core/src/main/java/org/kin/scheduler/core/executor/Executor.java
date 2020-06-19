package org.kin.scheduler.core.executor;

import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.RpcMessageCallContext;
import org.kin.kinrpc.message.core.ThreadSafeRpcEndpoint;
import org.kin.scheduler.core.driver.transport.CancelTask;
import org.kin.scheduler.core.driver.transport.KillExecutor;
import org.kin.scheduler.core.driver.transport.SubmitTask;
import org.kin.scheduler.core.driver.transport.TaskStatusChanged;
import org.kin.scheduler.core.executor.domain.ExecutorState;
import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;
import org.kin.scheduler.core.executor.transport.RegisterExecutor;
import org.kin.scheduler.core.executor.transport.TaskSubmitResp;
import org.kin.scheduler.core.log.LogUtils;
import org.kin.scheduler.core.log.Loggers;
import org.kin.scheduler.core.log.TaskLoggerContext;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.TaskHandlers;
import org.kin.scheduler.core.transport.RPCResp;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class Executor extends ThreadSafeRpcEndpoint {
    private final String appName;
    /** 所属的workerId */
    private final String workerId;
    private final String executorId;
    /** Executor的线程池, task执行线程池 */
    private ExecutionContext executionContext;
    /** log路径 */
    private final String logPath;
    /** 日志信息 */
    private TaskLoggerContext taskLoggerContext;
    /** logger */
    private Logger log;
    /** schduler client */
    private RpcEndpointRef schedulerRef;
    /** worker client */
    private RpcEndpointRef workerRef;
    /**
     *
     */
    private volatile boolean isStopped;

    //--------------------------------------------------------------------
    /** 存储task执行runnable, 用于中断task执行 */
    private ConcurrentMap<String, List<TaskRunner>> taskId2TaskRunners = new ConcurrentHashMap<>();

    public Executor(RpcEnv rpcEnv, String appName, String workerId, String executorId,
                    String logPath,
                    String executorDriverBackendAddress, String executorWorkerBackendAddress, boolean isLocal) {
        super(rpcEnv);
        this.appName = appName;
        this.workerId = workerId;
        this.executorId = executorId;
        this.logPath = logPath;

        Object[] schedulerHostPort = NetUtils.parseIpPort(executorDriverBackendAddress);
        this.schedulerRef = rpcEnv.createEndpointRef(schedulerHostPort[0].toString(), (Integer) schedulerHostPort[1], appName);
        Object[] workerHostPort = NetUtils.parseIpPort(executorWorkerBackendAddress);
        this.workerRef = rpcEnv.createEndpointRef(workerHostPort[0].toString(), (Integer) workerHostPort[1], workerId);
    }

    @Override
    protected void onStart() {
        if (isStopped) {
            return;
        }
        super.onStart();

        executionContext = ExecutionContext.cache("executor-".concat(executorId));
        taskLoggerContext = new TaskLoggerContext(executorId);
        log = LogUtils.getExecutorLogger(logPath, workerId, executorId);
        taskLoggerContext.start();

        workerRef.send(ExecutorStateChanged.running(appName, executorId));

        schedulerRef.send(RegisterExecutor.of(workerId, executorId, ref()));
        log.info("executor({}) started", executorId);
    }

    @Override
    protected void onStop() {
        if (isStopped) {
            return;
        }
        super.onStop();

        isStopped = true;

        //移除与scheduler的通信
        rpcEnv.removeOutBox(schedulerRef.getEndpointAddress().getRpcAddress());

        executionContext.shutdown();
        taskLoggerContext.stop();

        log.info("executor({}) closed", executorId);
    }

    @Override
    public void receive(RpcMessageCallContext context) {
        super.receive(context);

        Serializable message = context.getMessage();
        if (message instanceof SubmitTask) {
            execTask(context, (((SubmitTask) message).getTaskDescription()));
        } else if (message instanceof CancelTask) {
            cancelTask(context, (CancelTask) message);
        } else if (message instanceof KillExecutor) {
            stop();
        }
    }

    public void start() {
        if (isStopped) {
            return;
        }
        rpcEnv.register(executorId, this);
    }

    public void stop() {
        if (isStopped) {
            return;
        }
        rpcEnv.unregister(executorId, this);
    }

    //-------------------------------------------------------------------------------------------------------------------------
    private void execTask(RpcMessageCallContext context, TaskDescription taskDescription) {
        TaskSubmitResp taskSubmitResp = execTask0(taskDescription, log);
        log.info("exec task({}) finished, resulst >>>> {}", taskDescription.getTaskId(), taskSubmitResp);
        context.reply(taskSubmitResp);
    }

    private TaskSubmitResp execTask0(TaskDescription taskDescription, Logger log) {
        if (!isStopped) {
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
                            return TaskSubmitResp.failure(taskDescription.getTaskId(), "Discard Later abort task");
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
                return TaskSubmitResp.success(taskDescription.getTaskId(), taskLogName, taskOutputName);
            } catch (Exception e) {
                cleanFinishedTask(taskDescription);
                return TaskSubmitResp.failure(taskDescription.getTaskId(), ExceptionUtils.getExceptionDesc(e));
            }
        }

        return TaskSubmitResp.failure(taskDescription.getTaskId(), String.format("executor(%s) stopped", executorId));
    }

    private void cancelTask(RpcMessageCallContext context, CancelTask cancelTask) {
        String taskId = cancelTask.getTaskId();
        log.info("task({}) cancel >>>>", taskId);
        if (!isStopped) {
            if (taskId2TaskRunners.containsKey(taskId)) {
                for (TaskRunner taskRunner : taskId2TaskRunners.get(taskId)) {
                    taskRunner.interrupt();
                }
                taskId2TaskRunners.remove(taskId);
                context.reply(RPCResp.success());
                return;
            }
            context.reply(RPCResp.failure(String.format("executor(%s) has not run task(%s)", executorId, taskId)));
            return;
        }
        context.reply(RPCResp.failure(String.format("executor(%s) stopped", executorId)));
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

    //-----------------------------------------------------------------------------------------------------------------

    public void executorStateChanged(ExecutorState state) {
        switch (state) {
            case KILLED:
                workerRef.send(ExecutorStateChanged.kill(appName, executorId));
                break;
            case FAIL:
                workerRef.send(ExecutorStateChanged.fail(appName, executorId));
                break;
            case EXIT:
                workerRef.send(ExecutorStateChanged.exit(appName, executorId));
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

        private TaskRunner(TaskDescription taskDescription) {
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

                schedulerRef.send(execResult);
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

        private void interrupt() {
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
