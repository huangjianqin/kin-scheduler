package org.kin.scheduler.core.driver.schedule.impl;

import org.kin.scheduler.core.driver.Job;
import org.kin.scheduler.core.driver.route.RouteStrategies;
import org.kin.scheduler.core.driver.route.RouteStrategyType;
import org.kin.scheduler.core.driver.schedule.TaskContext;
import org.kin.scheduler.core.driver.schedule.TaskExecFuture;
import org.kin.scheduler.core.driver.schedule.TaskScheduler;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.worker.ExecutorContext;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public class SimpleTaskSchedulerImpl extends TaskScheduler<Task> {
    private AtomicInteger taskIdCounter = new AtomicInteger(1);

    public SimpleTaskSchedulerImpl(Job job) {
        super(job);
    }

    private void assignTaskId(Task task) {
        task.setJobId(job.getJobId());
        task.setTaskId(task.getJobId().concat("-Task").concat(String.valueOf(taskIdCounter.getAndIncrement())));
    }

    @Override
    public <R> TaskExecFuture<R> submitTask(Task task) {
        assignTaskId(task);
        TaskContext taskContext = taskSetManager.init(Collections.singletonList(task)).get(0);

        ExecutorContext selected = getAvailableExecutors(taskContext, RouteStrategies.getByName(RouteStrategyType.Random));
        if (Objects.nonNull(selected)) {
            return submitTask(selected, taskContext);
        }
        return null;
    }
}
