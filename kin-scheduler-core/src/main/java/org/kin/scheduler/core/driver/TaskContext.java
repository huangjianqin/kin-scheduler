package org.kin.scheduler.core.driver;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.driver.impl.JobTaskScheduler;
import org.kin.scheduler.core.task.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-02-10
 */
public class TaskContext {
    private static final int TASK_FINISH = 1;
    private static final int MAX_RETRY_TIMES = 3;

    private Task task;
    private int status;
    private Object result;
    //执行过该task的ExecutorId
    //最后一个item表示正执行该task的ExecutorId
    private List<String> execedExecutorIds = new ArrayList<>();
    private int retryTimes;
    private TaskSubmitFuture future;
    private JobTaskScheduler.TaskExecRunnable execRunnable;

    public TaskContext(Task task) {
        this.task = task;
    }

    public void submitTask(TaskSubmitFuture future, JobTaskScheduler.TaskExecRunnable execRunnable) {
        this.future = future;
        this.execRunnable = execRunnable;
    }

    public void exec(String executorId) {
        execedExecutorIds.add(executorId);
    }

    public boolean isFinish() {
        return status == TASK_FINISH;
    }

    public void finish(Object result) {
        if (isUnFinish()) {
            this.result = result;
            this.status = TASK_FINISH;
            synchronized (future) {
                future.notifyAll();
            }
        }
    }

    public boolean isUnFinish() {
        return status != TASK_FINISH;
    }

    /**
     * 正执行该task的ExecutorId
     */
    public String getExecingTaskExecutorId() {
        if (CollectionUtils.isNonEmpty(execedExecutorIds)) {
            return execedExecutorIds.get(execedExecutorIds.size() - 1);
        }

        return null;
    }

    public boolean retry() {
        retryTimes++;
        return retryTimes <= MAX_RETRY_TIMES;
    }

    //getter
    public Task getTask() {
        return task;
    }

    public Object getResult() {
        return result;
    }

    public List<String> getExecedExecutorIds() {
        return execedExecutorIds;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public TaskSubmitFuture getFuture() {
        return future;
    }

    public JobTaskScheduler.TaskExecRunnable getExecRunnable() {
        return execRunnable;
    }
}
