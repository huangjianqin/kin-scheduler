package org.kin.scheduler.core.driver.scheduler;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.executor.transport.TaskSubmitResult;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.domain.TaskStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * task 上下文
 *
 * @author huangjianqin
 * @date 2020-02-10
 */
public class TaskContext {
    /** task描述 */
    private TaskDescription taskDescription;
    /** task状态 */
    private TaskStatus state = TaskStatus.LAUNCHING;
    /** task执行结果 */
    private Serializable result;
    /**
     * 执行过该task的ExecutorId
     * 最后一个item表示正执行该task的ExecutorId
     */
    private List<String> execedExecutorIds = new ArrayList<>();
    /** 正在执行该task的executor id */
    private String runningExecutorId;
    /** 正在执行该task的executor所属的worker id */
    private String workerId;
    /** 该task的log路径 */
    private String logPath;
    /** 该task的output路径 */
    private String outputPath;

    private TaskExecFuture future;

    public TaskContext(TaskDescription taskDescription) {
        this.taskDescription = taskDescription;
    }

    public void submitTask(TaskSubmitResult submitResult, TaskExecFuture future) {
        this.logPath = submitResult.getLogPath();
        this.outputPath = submitResult.getOutputPath();
        this.future = future;
    }

    public void preExecute(String workerId, String runningExecutorId) {
        this.workerId = workerId;
        execedExecutorIds.add(runningExecutorId);
        this.runningExecutorId = runningExecutorId;
    }

    public void execFail() {
        this.runningExecutorId = null;
    }

    public boolean isFinish() {
        return state.isFinished();
    }

    public void finish(String taskId, TaskStatus taskStatus, Serializable result, String reason) {
        if (isNotFinish()) {
            this.result = result;
            this.state = taskStatus;
            future.done(taskId, taskStatus, result, logPath, outputPath, reason);
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

    public TaskDescription getTaskDescription() {
        return taskDescription;
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

    public String getRunningExecutorId() {
        return runningExecutorId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getLogPath() {
        return logPath;
    }

    public String getOutputPath() {
        return outputPath;
    }
}
