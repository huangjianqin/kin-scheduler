package org.kin.scheduler.core.driver.scheduler;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.executor.ExecutorBackend;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.task.domain.TaskStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * task 上下文
 * @author huangjianqin
 * @date 2020-02-10
 */
public class TaskContext {
    /** task描述 */
    private Task task;
    /** task状态 */
    private TaskStatus state;
    /** task执行结果 */
    private Serializable result;
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
        return state.isFinished();
    }

    public void finish(String taskId, TaskStatus taskStatus, Serializable result, String logFileName, String reason) {
        if (isNotFinish()) {
            this.result = result;
            this.state = taskStatus;
            future.done(taskId, taskStatus, result, logFileName, reason);
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
            this.state = TaskStatus.CANCELLED;
            future.done(task.getTaskId(), this.state, null, "", "task cancelled");
        }
    }


    public Task getTask() {
        return task;
    }

    public Serializable getResult() {
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
