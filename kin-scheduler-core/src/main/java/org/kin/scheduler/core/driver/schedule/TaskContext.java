package org.kin.scheduler.core.driver.schedule;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.executor.ExecutorBackend;
import org.kin.scheduler.core.executor.transport.TaskExecResult;
import org.kin.scheduler.core.task.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-02-10
 */
public class TaskContext {
    private static final int TASK_FINISH = 1;
    private static final int TASK_CANCELED = 2;

    private Task task;
    private int status;
    private Object result;
    /**
     * 执行过该task的ExecutorId
     * 最后一个item表示正执行该task的ExecutorId
     */
    private List<String> execedExecutorIds = new ArrayList<>();
    /**
     * 正在执行该task的executor
     */
    private ExecutorBackend executorBackend;
    private TaskExecFuture future;

    public TaskContext(Task task) {
        this.task = task;
    }

    public void submitTask(TaskExecFuture future) {
        this.future = future;
    }

    public void exec(String executorId, ExecutorBackend executorBackend) {
        execedExecutorIds.add(executorId);
        this.executorBackend = executorBackend;
    }

    public void execFail() {
        this.executorBackend = null;
    }

    public boolean isFinish() {
        return status == TASK_FINISH || status == TASK_CANCELED;
    }

    public void finish(TaskExecResult execResult) {
        if (isNotFinish()) {
            this.result = execResult.getExecResult();
            this.status = TASK_FINISH;
            future.done(execResult);
        }
    }

    public boolean isNotFinish() {
        return !isFinish();
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

    public void cancel() {
        if (isNotFinish()) {
            this.status = TASK_CANCELED;
            future.done(null);
        }
    }


    public Task getTask() {
        return task;
    }

    public Object getResult() {
        return result;
    }

    public List<String> getExecedExecutorIds() {
        return execedExecutorIds;
    }

    public TaskExecFuture getFuture() {
        return future;
    }

    public ExecutorBackend getExecutorBackend() {
        return executorBackend;
    }
}
