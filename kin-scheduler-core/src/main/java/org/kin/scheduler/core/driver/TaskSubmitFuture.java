package org.kin.scheduler.core.driver;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author huangjianqin
 * @date 2020-02-12
 */
public class TaskSubmitFuture<R> implements Future<R> {
    private TaskSetManager taskSetManager;
    private TaskContext taskContext;

    public TaskSubmitFuture(TaskContext taskContext) {
        this.taskContext = taskContext;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return taskSetManager.cancelTask(taskContext.getTask().getTaskId());
    }

    @Override
    public boolean isCancelled() {
        return taskSetManager.hasTask(taskContext.getTask().getTaskId());
    }

    @Override
    public boolean isDone() {
        return taskContext.isFinish();
    }

    @Override
    public R get() throws InterruptedException, ExecutionException {
        if (!isDone()) {
            synchronized (this){
                wait();
            }
        }
        return (R) taskContext.getResult();
    }

    @Override
    public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!isDone()) {
            synchronized (this){
                wait(unit.toMillis(timeout));
            }
        }
        return (R) taskContext.getResult();
    }
}
