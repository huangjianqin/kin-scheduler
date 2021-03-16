package org.kin.scheduler.core.driver.scheduler;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.SysUtils;
import org.kin.scheduler.core.executor.transport.TaskSubmitResp;
import org.kin.scheduler.core.task.domain.TaskStatus;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 执行task的future
 *
 * @author huangjianqin
 * @date 2020-02-12
 */
@SuppressWarnings("rawtypes")
public class TaskExecFuture<R extends Serializable> implements Future<R> {
    /** callback执行线程池 */
    public static ExecutionContext CALLBACK_EXECUTORS = ExecutionContext.fix(SysUtils.CPU_NUM, "TaskSubmitFuture-Callback-Thread");

    static {
        JvmCloseCleaner.DEFAULT().add(CALLBACK_EXECUTORS::shutdown);
    }

    /** task submit 返回结果 */
    private TaskSubmitResp taskSubmitResp;
    /** 所属TaskScheduler */
    private TaskScheduler taskScheduler;

    /** task id */
    private volatile String taskId;
    /** task status */
    private volatile TaskStatus taskStatus;
    /** task执行结果 */
    private volatile R result;
    /** task log path */
    private volatile String logPath;
    /** task output path */
    private volatile String outputPath;
    /** task fail reason */
    private volatile String reason;

    /** future是否完成 */
    private volatile boolean done;
    /** future是否取消 */
    private volatile boolean canneled;
    /** 添加的callbacks */
    private final Collection<TaskExecCallback<R>> callbacks = new CopyOnWriteArrayList<>();
    /** 阻塞等待的的线程数 */
    private short waiters;

    public TaskExecFuture(TaskSubmitResp taskSubmitResp, TaskScheduler taskScheduler) {
        this.taskSubmitResp = taskSubmitResp;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        canneled = taskScheduler.cancelTask(taskSubmitResp.getTaskId());
        return canneled;
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
        return (R) result;
    }

    @Override
    public R get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
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
        return (R) result;
    }

    /**
     * task 完成调用
     */
    @SuppressWarnings("unchecked")
    public void done(String taskId, TaskStatus taskStatus, Serializable result, String logPath, String outputPath, String reason) {
        done = true;

        this.taskId = taskId;
        this.taskStatus = taskStatus;
        this.result = (R) result;
        this.logPath = logPath;
        this.outputPath = outputPath;
        this.reason = reason;

        CALLBACK_EXECUTORS.execute(() -> {
            for (TaskExecCallback<R> callback : callbacks) {
                callback.execFinish(taskId, taskStatus, this.result, logPath, outputPath, reason);
            }
        });
        synchronized (this) {
            if (waiters > 0) {
                notifyAll();
            }
        }
    }

    /**
     * task 调度失败
     */
    public void fail(String taskId, TaskStatus taskStatus, String reason) {
        done(taskId, taskStatus, null, "", "", reason);
    }

    /**
     * 添加callback
     */
    public TaskExecFuture<R> addCallback(TaskExecCallback<R> callback) {
        if (isDone()) {
            CALLBACK_EXECUTORS.execute(() -> {
                callback.execFinish(taskId, taskStatus, this.result, logPath, outputPath, reason);
            });
        }
        callbacks.add(callback);
        return this;
    }

    /**
     * 批量添加callback
     */
    public TaskExecFuture<R> addCallback(TaskExecCallback<R>... callbacks) {
        for (TaskExecCallback<R> callback : callbacks) {
            addCallback(callback);
        }
        return this;
    }

    /**
     * @return task submit 返回结果
     */
    public TaskSubmitResp getTaskSubmitResp() {
        return taskSubmitResp;
    }
}