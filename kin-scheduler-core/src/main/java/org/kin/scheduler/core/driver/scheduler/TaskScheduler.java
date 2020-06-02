package org.kin.scheduler.core.driver.scheduler;

import org.kin.framework.service.AbstractService;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.driver.SchedulerBackend;
import org.kin.scheduler.core.driver.route.RouteStrategy;
import org.kin.scheduler.core.driver.transport.ExecutorRegisterInfo;
import org.kin.scheduler.core.driver.transport.TaskStatusChanged;
import org.kin.scheduler.core.executor.ExecutorBackend;
import org.kin.scheduler.core.executor.transport.TaskSubmitResult;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.domain.TaskStatus;
import org.kin.scheduler.core.transport.RPCResult;
import org.kin.scheduler.core.worker.ExecutorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-10
 * <p>
 * 提供一种规范, 提供调度task的接口, 具体对外调度api由子类实现
 */
public abstract class TaskScheduler<T> extends AbstractService implements SchedulerBackend {
    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    /** SchedulerBackend服务配置 */
    private ServiceConfig schedulerBackendServiceConfig;
    /** 已注册executors */
    private volatile Map<String, ExecutorContext> executors;
    /** application配置 */
    protected Application app;
    /** task集合 */
    protected TaskSetManager taskSetManager;
    private short waiters;

    public TaskScheduler(Application app) {
        super(app.getAppName().concat("-TaskScheduler"));
        this.app = app;
    }

    @Override
    public void init() {
        super.init();
        executors = Collections.emptyMap();
        taskSetManager = new TaskSetManager();

        schedulerBackendServiceConfig = Services.service(this, SchedulerBackend.class)
                .appName(getName().concat("-ExecutorDriverService"))
                .bind(app.getDriverPort())
                .actorLike();
        try {
            schedulerBackendServiceConfig.export();
        } catch (Exception e) {
            log.error("executor driver service encounter error >>> ", e);
        }
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        //取消所有未执行完的task
        for (TaskContext taskContext : taskSetManager.getAllUnFinishTask()) {
            String runningExecutorId = taskContext.getRunningExecutorId();
            ExecutorContext runningExecutorContext = executors.get(runningExecutorId);
            if (Objects.nonNull(runningExecutorContext)) {
                try {
                    runningExecutorContext.cancelTask(taskContext.getTaskDescription().getTaskId());
                } catch (Exception e) {
                    log.error("", e);
                }
            }
        }
        for (ExecutorContext executorContext : executors.values()) {
            executorContext.destroy();
        }
        schedulerBackendServiceConfig.disable();
    }

    public abstract <R extends Serializable> TaskExecFuture<R> submitTask(T task);

    protected final <R extends Serializable> TaskExecFuture<R> submitTask(ExecutorContext ec, TaskContext taskContext) {
        if (!isInState(State.STARTED)) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {

                }
            }
        }
        TaskDescription taskDescription = taskContext.getTaskDescription();
        taskContext.preExecute(ec.getWorkerId(), ec.getExecutorId());
        TaskSubmitResult submitResult = ec.execTask(taskDescription);
        if (Objects.nonNull(submitResult)) {
            if (submitResult.isSuccess()) {
                TaskExecFuture future = new TaskExecFuture(submitResult, taskSetManager, taskContext);
                taskContext.submitTask(submitResult, future);
                log.info("submitTask >>>> {}", taskContext.getTaskDescription());
                return future;
            } else {
                taskContext.execFail();
            }
        }
        return null;
    }

    @Override
    public RPCResult registerExecutor(ExecutorRegisterInfo executorRegisterInfo) {
        String workerId = executorRegisterInfo.getWorkerId();
        String executorId = executorRegisterInfo.getExecutorId();
        if (isInState(State.INITED) || isInState(State.STARTED)) {
            ExecutorContext executorContext = new ExecutorContext(workerId, executorId);
            ReferenceConfig<ExecutorBackend> executorBackendReferenceConfig = References.reference(ExecutorBackend.class)
                    .appName(getName().concat("-").concat(executorId))
                    .urls(executorRegisterInfo.getAddress());
            executorContext.start(executorBackendReferenceConfig);

            Map<String, ExecutorContext> tmpExecutors = new HashMap<>(this.executors);
            tmpExecutors.put(executorId, executorContext);
            executors = tmpExecutors;
            log.info("executor('{}') registered", executorId);
            if (waiters > 0) {
                //等待所有executor都注册完才开始调度task
                synchronized (this) {
                    notifyAll();
                }
            }
            return RPCResult.success();
        }

        return RPCResult.failure("");
    }

    @Override
    public final void taskStatusChange(TaskStatusChanged taskStatusChanged) {
        if (isInState(State.STARTED)) {
            String taskId = taskStatusChanged.getTaskId();
            if (taskSetManager.hasTask(taskId)) {
                TaskStatus state = taskStatusChanged.getStatus();

                TaskContext taskInfo = taskSetManager.getTaskInfo(taskId);
                if (state == TaskStatus.LOST) {
                    //移除该executor
                    String executorId = taskInfo.getExecingTaskExecutorId();
                    executorStatusChange(Collections.singletonList(executorId));
                }
                if (state.isFinished()) {
                    Serializable execResult = taskStatusChanged.getExecResult();
                    String reason = taskStatusChanged.getReason();

                    //TODO 考虑重试
                    taskSetManager.taskFinish(taskId, state, execResult, taskStatusChanged.getLogFileName(), reason);
                    log.info("Task(taskId={}) finished, state: {}, reason: {}, result >>>> {}", taskId, state, reason, execResult);
                }
            } else {
                log.error("unknown taskId '{}'", taskId);
            }
        }
    }

    public final boolean cancelTask(String taskId) {
        return taskSetManager.cancelTask(taskId);
    }

    public final void executorStatusChange(List<String> unAvailableExecutorIds) {
        if (isInState(State.INITED) || isInState(State.STARTED)) {
            //只管关闭无用Executor, 新Executor等待master分配好后, 新Executor会重新注册
            Map<String, ExecutorContext> executorContexts = new HashMap<>(this.executors);
            for (String unAvailableExecutorId : unAvailableExecutorIds) {
                ExecutorContext executorContext = executorContexts.remove(unAvailableExecutorId);
                if (Objects.nonNull(executorContext)) {
                    executorContext.destroy();
                }
            }
            this.executors = executorContexts;
        }
    }

    private void incrWaiter() {
        waiters++;
    }

    private void descWaiter() {
        waiters--;
    }

    /**
     * @return 可用Executor
     */
    protected final Collection<ExecutorContext> getAvailableExecutors() {
        if (executors.size() <= 0) {
            synchronized (this) {
                if (executors.size() <= 0) {
                    incrWaiter();
                    try {
                        wait();
                    } catch (InterruptedException e) {

                    } finally {
                        descWaiter();
                    }
                }
            }
        }
        return executors.values();
    }

    /**
     * @param taskContext task上下文信息
     * @return 可用Executor(过滤掉执行该task失败的)executor
     */
    protected final Collection<ExecutorContext> getAvailableExecutors(TaskContext taskContext) {
        return getAvailableExecutors().stream()
                .filter(ec -> !taskContext.getExecedExecutorIds().contains(ec.getExecutorId()))
                .collect(Collectors.toList());
    }

    /**
     * @param taskContext   task上下文信息
     * @param routeStrategy executor路由策略
     * @return 符合路由策略的executor(过滤掉执行该task失败的)executor
     */
    protected final ExecutorContext getAvailableExecutors(TaskContext taskContext, RouteStrategy routeStrategy) {
        return routeStrategy.route(getAvailableExecutors(taskContext));
    }

    /**
     * 等待所有task完成, 并结束
     */
    public void awaitTermination() {
        synchronized (taskSetManager) {
            try {
                taskSetManager.wait();
            } catch (InterruptedException e) {

            }
        }
    }

    /**
     * @return task上下文信息
     */
    public TaskContext getTaskInfo(String taskId) {
        return taskSetManager.getTaskInfo(taskId);
    }

    protected Map<String, ExecutorContext> getExecutors() {
        return executors;
    }

    //-----------------------------------------------------------------------------------------------------------------
    public class TaskSetManager {
        private Map<String, TaskContext> taskContexts = new ConcurrentHashMap<>();

        public List<TaskContext> init(Collection<TaskDescription> taskDescriptions) {
            List<TaskContext> taskContexts = new ArrayList<>();
            synchronized (this) {
                for (TaskDescription taskDescription : taskDescriptions) {
                    TaskContext taskContext = new TaskContext(taskDescription);
                    this.taskContexts.put(taskDescription.getTaskId(), taskContext);

                    taskContexts.add(taskContext);
                }
            }

            return taskContexts;
        }

        public boolean isAllFinish() {
            return taskContexts.values().stream().allMatch(TaskContext::isFinish);
        }

        public TaskContext getTaskInfo(String taskId) {
            return taskContexts.get(taskId);
        }

        public boolean hasTask(String taskId) {
            return taskContexts.containsKey(taskId);
        }

        public List<TaskContext> getAllUnFinishTask() {
            return taskContexts.values().stream().filter(TaskContext::isNotFinish).collect(Collectors.toList());
        }

        public boolean cancelTask(String taskId) {
            TaskContext taskContext = taskContexts.get(taskId);
            if (Objects.nonNull(taskContext) && taskContext.isNotFinish()) {
                ExecutorContext runningExecutorContext = executors.get(taskContext.getRunningExecutorId());
                if (Objects.nonNull(runningExecutorContext)) {
                    RPCResult result = runningExecutorContext.cancelTask(taskContext.getTaskDescription().getTaskId());
                    taskFinish(taskId, TaskStatus.CANCELLED, null, "", "task cancelled");
                    return result.isSuccess();
                }
            }

            return false;
        }

        public void taskFinish(String taskId, TaskStatus taskStatus, Serializable result, String logFileName, String reason) {
            TaskContext taskContext;
            synchronized (this) {
                taskContext = taskContexts.remove(taskId);
            }
            if (Objects.nonNull(taskContext) && taskContext.isNotFinish()) {
                taskContext.finish(taskId, taskStatus, result, logFileName, reason);
                tryTermination();
            }
        }

        private void tryTermination() {
            if (isAllFinish()) {
                synchronized (this) {
                    notifyAll();
                }
            }
        }
    }

}
