package org.kin.scheduler.core.driver;

import org.kin.scheduler.core.task.Task;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-10
 */
public class TaskSetManager {
    protected Map<String, TaskContext> taskContexts = new ConcurrentHashMap<>();

    public List<TaskContext> init(Collection<Task> tasks) {
        List<TaskContext> taskContexts = new ArrayList<>();
        for (Task task : tasks) {
            TaskContext taskContext = new TaskContext(task);
            this.taskContexts.put(task.getTaskId(), taskContext);

            taskContexts.add(taskContext);
        }

        return taskContexts;
    }

    public void taskFinish(String taskId, Object result) {
        TaskContext taskContext = taskContexts.get(taskId);
        if (Objects.nonNull(taskContext)) {
            taskContext.finish(result);
        }
    }

    public boolean isAllFinish() {
        return taskContexts.values().stream().allMatch(TaskContext::isFinish);
    }

    public TaskContext getTaskInfo(String taskId) {
        return taskContexts.get(taskId);
    }

    public boolean hasTask(String taskId) {
        return taskContexts.get(taskId) == null;
    }

    public List<TaskContext> getAllUnFinishTask() {
        return taskContexts.values().stream().filter(TaskContext::isUnFinish).collect(Collectors.toList());
    }

    public boolean cancelTask(String taskId) {
        TaskContext taskContext = taskContexts.remove(taskId);
        if (Objects.nonNull(taskContext) && taskContext.isUnFinish()) {
            taskContext.getExecRunnable().interrupt();
            taskContext.getFuture().notifyAll();
            return true;
        }

        return false;
    }
}
