package org.kin.scheduler.core.driver;

import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.scheduler.core.driver.domain.ExecutorRegisterInfo;
import org.kin.scheduler.core.executor.ExecutorBackend;
import org.kin.scheduler.core.executor.domain.TaskExecResult;
import org.kin.scheduler.core.executor.domain.TaskSubmitResult;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.worker.ExecutorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2020-02-10
 * <p>
 * 提供一种规范, 提供调度task的接口, 具体对外调度api由子类实现
 */
public abstract class TaskScheduler extends AbstractService {
    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    protected Job job;
    private volatile Map<String, ExecutorContext> executorContexts;
    protected TaskSetManager taskSetManager;
    private short waiters;

    public TaskScheduler(Job job) {
        super(job.getJobId().concat("-TaskScheduler"));
        this.job = job;
    }

    @Override
    public void init() {
        super.init();
        executorContexts = new HashMap<>();
        taskSetManager = new TaskSetManager();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        //取消所有未执行完的task
        if (Objects.nonNull(taskSetManager)) {
            for (TaskContext taskContext : taskSetManager.getAllUnFinishTask()) {
                ExecutorBackend executorBackend = taskContext.getExecutorBackend();
                if (Objects.nonNull(executorBackend)) {
                    executorBackend.cancelTask(taskContext.getTask().getTaskId());
                }
            }
        }
        if (CollectionUtils.isNonEmpty(executorContexts)) {
            for (ExecutorContext executorContext : executorContexts.values()) {
                executorContext.destroy();
            }
        }
    }

    protected final <R> TaskExecFuture<R> submitTask(ExecutorContext ec, TaskContext taskContext) {
        if (isInState(State.STARTED)) {
            Task task = taskContext.getTask();
            taskContext.exec(ec.getExecutorId(), ec);
            TaskSubmitResult submitResult = ec.execTask(task);
            if (Objects.nonNull(submitResult)) {
                if (submitResult.isSuccess()) {
                    TaskExecFuture future = new TaskExecFuture(submitResult, taskSetManager, taskContext);
                    taskContext.submitTask(future);
                    log.debug("submitTask >>>> {}", taskContext.getTask());
                    return future;
                } else {
                    taskContext.execFail();
                }
            }
        }
        return null;
    }

    public final boolean registerExecutor(ExecutorRegisterInfo executorRegisterInfo) {
        String executorId = executorRegisterInfo.getExecutorId();
        if (isInState(State.INITED) || isInState(State.STARTED)) {
            ExecutorContext executorContext = new ExecutorContext(executorId);
            ReferenceConfig<ExecutorBackend> executorBackendReferenceConfig = References.reference(ExecutorBackend.class)
                    .appName(getName().concat("-").concat(executorId))
                    .urls(executorRegisterInfo.getAddress());
            executorContext.start(executorBackendReferenceConfig);
            executorContexts.put(executorId, executorContext);
            if (waiters > 0) {
                //等待所有executor都注册完才开始调度task
                synchronized (this) {
                    notifyAll();
                }
            }
            return true;
        }

        return false;
    }

    public final void taskFinish(TaskExecResult execResult) {
        if (isInState(State.STARTED)) {
            String taskId = execResult.getTaskId();
            if (taskSetManager.hasTask(taskId)) {
                if (execResult.isSuccess()) {
                    taskSetManager.taskFinish(execResult);
                    log.info("Task(taskId={}) finished, result >>>> {}", taskId, execResult.getExecResult());
                }
            } else {
                log.error("unknown taskId '{}'", taskId);
            }
        }
    }

    public final void executorStatusChange(List<String> unAvailableExecutorIds) {
        if (isInState(State.INITED) || isInState(State.STARTED)) {
            //只管关闭无用Executor, 新Executor等待master分配好后, master通知新Executor给Driver注册
            Map<String, ExecutorContext> executorContexts = new HashMap<>(this.executorContexts);
            for (String unAvailableExecutorId : unAvailableExecutorIds) {
                ExecutorContext executorContext = executorContexts.remove(unAvailableExecutorId);
                if (Objects.nonNull(executorContext)) {
                    executorContext.destroy(false);
                }
            }
            this.executorContexts = executorContexts;
        }
    }

    protected void incrWaiter() {
        waiters++;
    }

    protected void descWaiter() {
        waiters--;
    }

    protected Collection<ExecutorContext> getAvailableExecutors() {
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
}
