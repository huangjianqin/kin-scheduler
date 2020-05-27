package org.kin.scheduler.core.driver.scheduler.impl;

import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.driver.route.RouteStrategies;
import org.kin.scheduler.core.driver.route.RouteStrategyType;
import org.kin.scheduler.core.driver.scheduler.TaskContext;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.driver.scheduler.TaskScheduler;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.worker.ExecutorContext;

import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public class SimpleTaskScheduler extends TaskScheduler<Task> {
    private AtomicInteger taskIdCounter = new AtomicInteger(1);

    public SimpleTaskScheduler(Application app) {
        super(app);
    }

    private void assignTaskId(Task task) {
        task.setJobId(task.getJobId());
        task.setTaskId(task.getJobId().concat("-Task").concat(String.valueOf(taskIdCounter.getAndIncrement())));
    }

    @Override
    public <R extends Serializable> TaskExecFuture<R> submitTask(Task task) {
        assignTaskId(task);
        TaskContext taskContext = taskSetManager.init(Collections.singletonList(task)).get(0);

        ExecutorContext selected = getAvailableExecutors(taskContext, RouteStrategies.getByName(RouteStrategyType.Random));
        if (Objects.nonNull(selected)) {
            return submitTask(selected, taskContext);
        }
        return null;
    }
}
