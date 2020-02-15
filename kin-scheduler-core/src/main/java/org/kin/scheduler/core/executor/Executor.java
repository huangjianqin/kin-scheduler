package org.kin.scheduler.core.executor;

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
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.TaskHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(Executor.class);

    private String executorId;
    //Executor暴露给worker的host
    private String backendHost;
    //Executor暴露给worker的端口
    private int backendPort;
    //Executor的并行数
    private int parallelism;
    //Executor的线程池
    private PartitionTaskExecutor<String> threads;
    //Executor加载的处理器
    private final TaskHandlers taskHandlers = new TaskHandlers();
    //rpc服务配置
    private ServiceConfig serviceConfig;

    //--------------------------------------------------------------------
    private ConcurrentMap<String, List<TaskRunner>> taskId2TaskRunners = new ConcurrentHashMap<>();

    public Executor(String executorId, String backendHost, int backendPort) {
        this(executorId, backendHost, backendPort, SysUtils.CPU_NUM);
    }

    public Executor(String executorId, String backendHost, int backendPort, int parallelism) {
        super("Executor-".concat(executorId));
        this.executorId = executorId;
        this.backendHost = backendHost;
        this.backendPort = backendPort;
        this.parallelism = parallelism;
    }

    @Override
    public void init() {
        super.init();
        this.threads = new PartitionTaskExecutor<String>(parallelism, EfficientHashPartitioner.INSTANCE);
        this.taskHandlers.init();

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
        synchronized (this){
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }

    @Override
    public TaskExecResult execTask(Task task) {
        if (this.isInState(State.STARTED)) {
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

                Future future = threads.execute(task.getTaskId(), newTaskRunner);

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

                Object execResult;
                if (task.getTimeout() > 0) {
                    try {
                        execResult = future.get(task.getTimeout(), TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        execResult = null;
                        cleanFinishedTask(task);
                        return TaskExecResult.failure("execute time out");
                    }
                } else if (task.getTimeout() == 0) {
                    execResult = future.get();
                } else {
                    execResult = null;
                }

                return TaskExecResult.success(
                        task.getExecStrategy().getDesc()
                                .concat("run task >>>> ")
                                .concat(task.toString()),
                        execResult);
            } catch (Exception e) {
                cleanFinishedTask(task);
                return TaskExecResult.failure(ExceptionUtils.getExceptionDesc(e));
            }
        }

        return TaskExecResult.failureWithRetry("executor stopped");
    }

    @Override
    public RPCResult cancelTask(String taskId) {
        if (taskId2TaskRunners.containsKey(taskId)) {
            for (TaskRunner taskRunner : taskId2TaskRunners.get(taskId)) {
                taskRunner.interrupt();
            }
            taskId2TaskRunners.remove(taskId);
            return RPCResult.success();
        }
        return RPCResult.failure(String.format("executor(%s) has not run task(%s)", executorId, taskId));
    }

    @Override
    public void destroy() {
        stop();
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
        if(Objects.nonNull(serviceConfig)){
            serviceConfig.disable();
        }
        threads.shutdown();
    }

    //-----------------------------------------------------------------------------------------------------------------
    private class TaskRunner implements Callable {
        private Task task;
        private Lock lock = new ReentrantLock();
        private volatile boolean isStopped;
        private Thread currentThread;

        public TaskRunner(Task task) {
            this.task = task;
        }

        @Override
        public Object call() {
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
                TaskHandler taskHandler = taskHandlers.getTaskHandler(task);
                Preconditions.checkNotNull(taskHandler, "task handler is null");
                if (taskHandler != null) {
                    return taskHandler.exec(task);
                }
            } finally {
                isStopped = true;
                cleanFinishedTask(task);
            }

            return null;
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
