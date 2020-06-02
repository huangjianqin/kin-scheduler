package org.kin.scheduler.core.driver.scheduler;

import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.domain.TaskStatus;
import org.kin.scheduler.core.transport.RPCResult;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-10
 */
public class TaskSetManager {
    private Map<String, TaskContext> taskContexts = new ConcurrentHashMap<>();

    public List<TaskContext> init(Collection<TaskDescription> taskDescriptions) {
        List<TaskContext> taskContexts = new ArrayList<>();
        synchronized (this) {
            for (TaskDescription taskDescription : taskDescriptions) {
                TaskContext taskContext = new TaskContext(taskDescription);
                this.taskContexts.put(taskDescription.getTaskId(), taskContext);

                taskContexts.add(taskContext);
            }
        }

        return taskContexts;
    }

    public boolean isAllFinish() {
        return taskContexts.values().stream().allMatch(TaskContext::isFinish);
    }

    public TaskContext getTaskInfo(String taskId) {
        return taskContexts.get(taskId);
    }

    public boolean hasTask(String taskId) {
        return taskContexts.containsKey(taskId);
    }

    public List<TaskContext> getAllUnFinishTask() {
        return taskContexts.values().stream().filter(TaskContext::isNotFinish).collect(Collectors.toList());
    }

    public boolean cancelTask(String taskId) {
        TaskContext taskContext = taskContexts.get(taskId);
        if (Objects.nonNull(taskContext) && taskContext.isNotFinish()) {
            RPCResult result = taskContext.getExecutorBackend().cancelTask(taskContext.getTaskDescription().getTaskId());
            taskFinish(taskId, TaskStatus.CANCELLED, null, "", "task cancelled");
            return result.isSuccess();
        }

        return false;
    }

    public void taskFinish(String taskId, TaskStatus taskStatus, Serializable result, String logFileName, String reason) {
        TaskContext taskContext;
        synchronized (this) {
            taskContext = taskContexts.remove(taskId);
        }
        if (Objects.nonNull(taskContext) && taskContext.isNotFinish()) {
            taskContext.finish(taskId, taskStatus, result, logFileName, reason);
            tryTermination();
        }
    }

    private void tryTermination() {
        if (isAllFinish()) {
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
