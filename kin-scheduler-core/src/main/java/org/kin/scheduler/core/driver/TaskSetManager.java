package org.kin.scheduler.core.driver;

import org.kin.scheduler.core.domain.RPCResult;
import org.kin.scheduler.core.executor.domain.TaskExecResult;
import org.kin.scheduler.core.task.Task;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-10
 */
public class TaskSetManager {
    private Map<String, TaskContext> taskContexts = new ConcurrentHashMap<>();

    public List<TaskContext> init(Collection<Task> tasks) {
        List<TaskContext> taskContexts = new ArrayList<>();
        for (Task task : tasks) {
            TaskContext taskContext = new TaskContext(task);
            this.taskContexts.put(task.getTaskId(), taskContext);

            taskContexts.add(taskContext);
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
        TaskContext taskContext = taskContexts.remove(taskId);
        if (Objects.nonNull(taskContext) && taskContext.isNotFinish()) {
            RPCResult result = taskContext.getExecutorBackend().cancelTask(taskContext.getTask().getTaskId());
            return result.isSuccess();
        }

        return false;
    }

    public void taskFinish(TaskExecResult execResult) {
        String taskId = execResult.getTaskId();
        TaskContext taskContext = taskContexts.remove(taskId);
        if (Objects.nonNull(taskContext) && taskContext.isNotFinish()) {
            taskContext.finish(execResult);
        }
    }
}
