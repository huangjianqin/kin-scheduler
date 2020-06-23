package org.kin.scheduler.core.driver.scheduler;

import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.RpcMessageCallContext;
import org.kin.kinrpc.message.core.ThreadSafeRpcEndpoint;
import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.driver.ExecutorContext;
import org.kin.scheduler.core.driver.route.RouteStrategy;
import org.kin.scheduler.core.driver.transport.CancelTask;
import org.kin.scheduler.core.driver.transport.KillExecutor;
import org.kin.scheduler.core.driver.transport.SubmitTask;
import org.kin.scheduler.core.driver.transport.TaskStatusChanged;
import org.kin.scheduler.core.executor.transport.RegisterExecutor;
import org.kin.scheduler.core.executor.transport.TaskSubmitResp;
import org.kin.scheduler.core.master.transport.ExecutorStateUpdate;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.domain.TaskStatus;
import org.kin.scheduler.core.transport.RPCResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-10
 * <p>
 * 提供一种规范, 提供调度task的接口, 具体对外调度api由子类实现
 */
public abstract class TaskScheduler<T> extends ThreadSafeRpcEndpoint {
    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    /** 已注册executors, copy-on-write方式更新 */
    private volatile Map<String, ExecutorContext> executors;
    /** application配置 */
    protected Application app;
    /** task集合 */
    protected TaskSetManager taskSetManager;
    /** 阻塞等待executor足够分配task执行的线程数 */
    private short waiters;
    private volatile boolean isStopped;

    public TaskScheduler(RpcEnv rpcEnv, Application app) {
        super(rpcEnv);
        this.app = app;
    }

    @Override
    protected void onStart() {
        if (isStopped) {
            return;
        }
        super.onStart();

        executors = Collections.emptyMap();
        taskSetManager = new TaskSetManager();
    }

    @Override
    protected void onStop() {
        if (isStopped) {
            return;
        }
        super.onStop();

        isStopped = true;
        //取消所有未执行完的task
        for (TaskContext taskContext : taskSetManager.getAllUnFinishTask()) {
            String runningExecutorId = taskContext.getRunningExecutorId();
            ExecutorContext runningExecutorContext = executors.get(runningExecutorId);
            if (Objects.nonNull(runningExecutorContext)) {
                runningExecutorContext.ref().send(CancelTask.of(taskContext.getTaskDescription().getTaskId()));
            }
        }
        //kill executor
        for (ExecutorContext executorContext : executors.values()) {
            executorContext.ref().send(KillExecutor.INSTANCE);
            log.info("kill worker '{}'s executor '{}' address: {}", executorContext.getWorkerId(), executorContext.getExecutorId(), executorContext.getExecutorAddress());
        }
    }

    @Override
    public void receive(RpcMessageCallContext context) {
        super.receive(context);

        Serializable message = context.getMessage();
        if (message instanceof ExecutorStateUpdate) {
            executorStatusChange(((ExecutorStateUpdate) message).getUnavailableExecutorIds());
        } else if (message instanceof RegisterExecutor) {
            registerExecutor((RegisterExecutor) message);
        } else if (message instanceof TaskStatusChanged) {
            taskStatusChange((TaskStatusChanged) message);
        }
    }

    public void start() {
        if (isStopped) {
            return;
        }

        rpcEnv.register(app.getAppName(), this);
    }

    public void stop() {
        if (isStopped) {
            return;
        }

        rpcEnv.unregister(app.getAppName(), this);
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * master通知executor status change
     */
    private void executorStatusChange(List<String> unavailableExecutorIds) {
        if (!isStopped) {
            //只管关闭无用Executor, 新Executor等待master分配好后, 新Executor会重新注册
            Map<String, ExecutorContext> executorContexts = new HashMap<>(this.executors);
            for (String unAvailableExecutorId : unavailableExecutorIds) {
                ExecutorContext executorContext = executorContexts.remove(unAvailableExecutorId);
                if (Objects.nonNull(executorContext)) {
                    executorContext.ref().send(KillExecutor.INSTANCE);
                }
            }
            this.executors = executorContexts;
        }
    }

    /**
     * executor 往scheduler注册
     */
    private void registerExecutor(RegisterExecutor registerExecutor) {
        String workerId = registerExecutor.getWorkerId();
        String executorId = registerExecutor.getExecutorId();
        if (!isStopped) {
            ExecutorContext executorContext = new ExecutorContext(workerId, executorId, registerExecutor.getExecutorRef());

            Map<String, ExecutorContext> tmpExecutors = new HashMap<>(this.executors);
            tmpExecutors.put(executorId, executorContext);
            executors = tmpExecutors;
            log.info("executor('{}') registered", executorId);
            //有可用executor就释放等待足够executor线程
            synchronized (this) {
                if (waiters > 0) {
                    notifyAll();
                }
            }
        }
    }

    /**
     * executor 往scheduler通知task status变化
     */
    private void taskStatusChange(TaskStatusChanged taskStatusChanged) {
        if (!isStopped) {
            String taskId = taskStatusChanged.getTaskId();
            if (taskSetManager.hasTask(taskId)) {
                TaskStatus state = taskStatusChanged.getStatus();

                TaskContext taskInfo = taskSetManager.getTaskInfo(taskId);
                if (state == TaskStatus.LOST) {
                    //task丢失
                    //移除该executor
                    String executorId = taskInfo.getRunningTaskExecutorId();
                    executorStatusChange(Collections.singletonList(executorId));
                }
                if (state.isFinished()) {
                    //task完成
                    Serializable execResult = taskStatusChanged.getExecResult();
                    String reason = taskStatusChanged.getReason();

                    taskSetManager.taskFinish(taskId, state, execResult, reason);
                    log.info("Task(taskId={}) finished, state: {}, reason: {}, result >>>> {}", taskId, state, reason, execResult);
                }
                //TODO 考虑重试
            } else {
                log.error("unknown taskId '{}'", taskId);
            }
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------------

    private void incrWaiter() {
        waiters++;
    }

    private void descWaiter() {
        waiters--;
    }

    private void waitForExecutors() {
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
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * 提交task执行, 交给子类自定义实现
     * call线程
     */
    public abstract <R extends Serializable> TaskExecFuture<R> submitTask(T task);

    /**
     * 提交task执行, 通用接口
     * call线程
     */
    protected final <R extends Serializable> TaskExecFuture<R> submitTask(ExecutorContext ec, TaskContext taskContext) {
        if (!isStopped) {
            waitForExecutors();
        }
        TaskDescription taskDescription = taskContext.getTaskDescription();
        taskContext.preSubmit(ec.getWorkerId(), ec.getExecutorId());
        TaskSubmitResp submitResult = null;
        try {
            submitResult = (TaskSubmitResp) ec.ref().ask(SubmitTask.of(taskDescription)).get();
            if (Objects.nonNull(submitResult)) {
                if (submitResult.isSuccess()) {
                    TaskExecFuture<R> future = new TaskExecFuture<>(submitResult, this);
                    taskContext.submitTask(submitResult, future);
                    log.info("submitTask >>>> {}", taskContext.getTaskDescription());
                    return future;
                } else {
                    taskContext.execFail();
                }
            }
        } catch (InterruptedException e) {

        } catch (ExecutionException e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * 取消task
     * call线程
     */
    public final boolean cancelTask(String taskId) {
        return taskSetManager.cancelTask(taskId);
    }

    /**
     * @return 可用Executor
     */
    protected final Collection<ExecutorContext> getAvailableExecutors() {
        waitForExecutors();
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
        /** key -> task id, value -> task上下文 */
        private Map<String, TaskContext> taskContexts = new ConcurrentHashMap<>();

        /**
         * 初始化TaskContext
         */
        public TaskContext init(TaskDescription taskDescription) {
            TaskContext taskContext = new TaskContext(taskDescription);
            TaskContext oldContext = taskContexts.putIfAbsent(taskDescription.getTaskId(), taskContext);
            if (Objects.nonNull(oldContext)) {
                //已存在相同task id的task上下文
                return oldContext;
            }

            return taskContext;
        }

        /**
         * @return 是否所有submitted task已完成
         */
        private boolean isAllFinish() {
            return taskContexts.values().stream().allMatch(TaskContext::isFinish);
        }

        /**
         * 根据task id获取task 上下文
         */
        private TaskContext getTaskInfo(String taskId) {
            return taskContexts.get(taskId);
        }

        /**
         * 是否包含指定task id的task 上下文
         */
        private boolean hasTask(String taskId) {
            return taskContexts.containsKey(taskId);
        }

        /**
         * @return 所有未完成的task 上下文
         */
        private List<TaskContext> getAllUnFinishTask() {
            return taskContexts.values().stream().filter(TaskContext::isNotFinish).collect(Collectors.toList());
        }

        /**
         * 取消指定task id的task
         */
        private boolean cancelTask(String taskId) {
            TaskContext taskContext = taskContexts.get(taskId);
            if (Objects.nonNull(taskContext) && taskContext.isNotFinish()) {
                ExecutorContext runningExecutorContext = executors.get(taskContext.getRunningExecutorId());
                if (Objects.nonNull(runningExecutorContext)) {
                    boolean askResult = false;
                    try {
                        RPCResp result = (RPCResp) runningExecutorContext.ref().ask(CancelTask.of(taskContext.getTaskDescription().getTaskId())).get();
                        askResult = result.isSuccess();
                    } catch (Exception e) {
                        log.error("", e);
                    }
                    taskFinish(taskId, TaskStatus.CANCELLED, null, "task cancelled");
                    return askResult;
                }
            }

            return false;
        }

        /**
         * task 完成时调用
         */
        private void taskFinish(String taskId, TaskStatus taskStatus, Serializable result, String reason) {
            TaskContext taskContext;
            if (!app.isDropResult()) {
                taskContext = taskContexts.get(taskId);
            } else {
                synchronized (this) {
                    taskContext = taskContexts.remove(taskId);
                }
            }

            if (Objects.nonNull(taskContext) && taskContext.isNotFinish()) {
                taskContext.finish(taskId, taskStatus, result, reason);
                tryTermination();
            }
        }

        /**
         * 尝试释放等待所有task完成的线程
         */
        private void tryTermination() {
            if (isAllFinish()) {
                synchronized (this) {
                    notifyAll();
                }
            }
        }
    }

}
