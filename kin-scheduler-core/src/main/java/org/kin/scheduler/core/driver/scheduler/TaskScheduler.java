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
    private volatile Map<String, ExecutorContext> executorContexts;
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
        executorContexts = new HashMap<>();
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
            ExecutorBackend executorBackend = taskContext.getExecutorBackend();
            if (Objects.nonNull(executorBackend)) {
                try {
                    executorBackend.cancelTask(taskContext.getTaskDescription().getTaskId());
                } catch (Exception e) {
                    log.error("", e);
                }
            }
        }
        for (ExecutorContext executorContext : executorContexts.values()) {
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
        taskContext.exec(ec.getExecutorId(), ec);
        TaskSubmitResult submitResult = ec.execTask(taskDescription);
        if (Objects.nonNull(submitResult)) {
            if (submitResult.isSuccess()) {
                TaskExecFuture future = new TaskExecFuture(submitResult, taskSetManager, taskContext);
                taskContext.submitTask(future);
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
        String executorId = executorRegisterInfo.getExecutorId();
        if (isInState(State.INITED) || isInState(State.STARTED)) {
            ExecutorContext executorContext = new ExecutorContext(executorId);
            ReferenceConfig<ExecutorBackend> executorBackendReferenceConfig = References.reference(ExecutorBackend.class)
                    .appName(getName().concat("-").concat(executorId))
                    .urls(executorRegisterInfo.getAddress());
            executorContext.start(executorBackendReferenceConfig);
            executorContexts.put(executorId, executorContext);
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
            Map<String, ExecutorContext> executorContexts = new HashMap<>(this.executorContexts);
            for (String unAvailableExecutorId : unAvailableExecutorIds) {
                ExecutorContext executorContext = executorContexts.remove(unAvailableExecutorId);
                if (Objects.nonNull(executorContext)) {
                    executorContext.destroy();
                }
            }
            this.executorContexts = executorContexts;
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
        if (executorContexts.size() <= 0) {
            synchronized (this) {
                if (executorContexts.size() <= 0) {
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
        return executorContexts.values();
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
}
