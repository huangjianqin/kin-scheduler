package org.kin.scheduler.core.driver.scheduler;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.SysUtils;
import org.kin.scheduler.core.executor.transport.TaskSubmitResult;
import org.kin.scheduler.core.task.domain.TaskStatus;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-02-12
 * <p>
 * 等待task执行的future
 */
public class TaskExecFuture<R extends Serializable> implements Future<R> {
    private static ExecutionContext CALLBACK_EXECUTORS = ExecutionContext.fix(SysUtils.CPU_NUM, "TaskSubmitFuture-Callback-Thread-");

    static {
        JvmCloseCleaner.DEFAULT().add(CALLBACK_EXECUTORS::shutdown);
    }

    private TaskSubmitResult taskSubmitResult;
    private TaskSetManager taskSetManager;
    private TaskContext taskContext;
    private volatile boolean done;
    private boolean canneled;
    private Collection<TaskExecCallback<R>> callbacks = new CopyOnWriteArrayList<>();
    private short waiters;

    public TaskExecFuture(TaskSubmitResult taskSubmitResult, TaskSetManager taskSetManager, TaskContext taskContext) {
        this.taskSubmitResult = taskSubmitResult;
        this.taskSetManager = taskSetManager;
        this.taskContext = taskContext;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return taskSetManager.cancelTask(taskContext.getTaskDescription().getTaskId());
    }

    @Override
    public boolean isCancelled() {
        return !done && canneled;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public R get() throws InterruptedException, ExecutionException {
        if (!isDone()) {
            synchronized (this) {
                waiters++;
                try {
                    wait();
                } finally {
                    waiters--;
                }
            }
        }
        return (R) taskContext.getResult();
    }

    @Override
    public R get(long timeout, TimeUnit unit) throws InterruptedException {
        if (!isDone()) {
            synchronized (this) {
                waiters++;
                try {
                    wait(unit.toMillis(timeout));
                } finally {
                    waiters--;
                }
            }
        }
        return (R) taskContext.getResult();
    }

    public void done(String taskId, TaskStatus taskStatus, Serializable result, String logFileName, String reason) {
        done = true;
        CALLBACK_EXECUTORS.execute(() -> {
            for (TaskExecCallback<R> callback : callbacks) {
                callback.execFinish(taskId, taskStatus, (R) result, logFileName, reason);
            }
        });
        if (waiters > 0) {
            notifyAll();
        }
    }

    public TaskExecFuture<R> addCallback(TaskExecCallback<R> callback) {
        callbacks.add(callback);
        return this;
    }

    public TaskSubmitResult getTaskSubmitResult() {
        return taskSubmitResult;
    }
}