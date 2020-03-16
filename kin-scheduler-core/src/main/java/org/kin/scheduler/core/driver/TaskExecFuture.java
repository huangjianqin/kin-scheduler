package org.kin.scheduler.core.driver;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.SysUtils;
import org.kin.scheduler.core.executor.domain.TaskExecResult;
import org.kin.scheduler.core.executor.domain.TaskSubmitResult;

import java.util.Collection;
import java.util.Objects;
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
public class TaskExecFuture<R> implements Future<R> {
    private static ThreadManager CALLBACK_THREADS = ThreadManager.fix(SysUtils.CPU_NUM, "TaskSubmitFuture-Callback-Thread-");

    static {
        JvmCloseCleaner.DEFAULT().add(CALLBACK_THREADS::shutdown);
    }

    private TaskSubmitResult taskSubmitResult;
    private TaskSetManager taskSetManager;
    private TaskContext taskContext;
    private volatile boolean done;
    private boolean canneled;
    private Collection<TaskExecCallback> callbacks = new CopyOnWriteArrayList<>();
    private short waiters;

    public TaskExecFuture(TaskSubmitResult taskSubmitResult, TaskSetManager taskSetManager, TaskContext taskContext) {
        this.taskSubmitResult = taskSubmitResult;
        this.taskSetManager = taskSetManager;
        this.taskContext = taskContext;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return taskSetManager.cancelTask(taskContext.getTask().getTaskId());
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

    public void done(TaskExecResult execResult) {
        done = true;
        if (Objects.nonNull(execResult)) {
            CALLBACK_THREADS.execute(() -> {
                for (TaskExecCallback callback : callbacks) {
                    callback.execFinish(execResult);
                }
            });
        } else {
            canneled = true;
        }
        if (waiters > 0) {
            notifyAll();
        }
    }

    public TaskExecFuture addCallback(TaskExecCallback callback) {
        callbacks.add(callback);
        return this;
    }

    public TaskSubmitResult getTaskSubmitResult() {
        return taskSubmitResult;
    }
}