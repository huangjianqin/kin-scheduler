package org.kin.scheduler.core.driver.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.driver.Job;
import org.kin.scheduler.core.driver.TaskContext;
import org.kin.scheduler.core.driver.TaskExecFuture;
import org.kin.scheduler.core.driver.TaskScheduler;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.worker.ExecutorContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public class TaskSchedulerImpl extends TaskScheduler {
    private AtomicInteger taskIdCounter = new AtomicInteger(1);
    private Random random = new Random();

    public TaskSchedulerImpl(Job job) {
        super(job);
    }

    private void assignTaskId(Task task) {
        task.setJobId(job.getJobId());
        task.setTaskId(task.getJobId().concat("-Task").concat(String.valueOf(taskIdCounter.getAndIncrement())));
    }

    private ExecutorContext getSuitableExecutorBackend(Collection<ExecutorContext> availableExecutorContexts) {
        if (CollectionUtils.isNonEmpty(availableExecutorContexts)) {
            List<ExecutorContext> availableExecutorContextList = new ArrayList<>(availableExecutorContexts);
            return availableExecutorContextList.get(random.nextInt(availableExecutorContextList.size()));
        }
        return null;
    }

    public final <R> TaskExecFuture<R> submitTask(Task task) {
        if (!isInState(State.STARTED)) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {

                }
            }
        }
        assignTaskId(task);
        TaskContext taskContext = taskSetManager.init(Collections.singletonList(task)).get(0);

        //过滤掉已经执行过该task的executor
        List<ExecutorContext> filterExecutorContexts = getAvailableExecutors().stream()
                .filter(ec -> !taskContext.getExecedExecutorIds().contains(ec.getExecutorId()))
                .collect(Collectors.toList());

        ExecutorContext selected = getSuitableExecutorBackend(filterExecutorContexts);
        if (Objects.nonNull(selected)) {
            return submitTask(selected, taskContext);
        }
        return null;

    }
}
