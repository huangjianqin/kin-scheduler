package org.kin.scheduler.core.impl;

import org.kin.scheduler.core.driver.Job;
import org.kin.scheduler.core.driver.TaskContext;
import org.kin.scheduler.core.driver.TaskExecFuture;
import org.kin.scheduler.core.driver.TaskScheduler;
import org.kin.scheduler.core.driver.route.RouteStrategies;
import org.kin.scheduler.core.driver.route.RouteStrategy;
import org.kin.scheduler.core.driver.route.RouteStrategyType;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.worker.ExecutorContext;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-05-16
 */
public class SimpleTaskSchedulerImpl extends TaskScheduler {
    public SimpleTaskSchedulerImpl(Job job) {
        super(job);
    }

    public <R, PARAM extends Serializable> TaskExecFuture<R> submitTask(Task<PARAM> task) {
        TaskContext taskContext = taskSetManager.init(Collections.singletonList(task)).get(0);

        //过滤掉已经执行过该task的executor
        List<ExecutorContext> filterExecutorContexts = getAvailableExecutors().stream()
                .filter(ec -> !taskContext.getExecedExecutorIds().contains(ec.getExecutorId()))
                .collect(Collectors.toList());

        RouteStrategy routeStrategy = RouteStrategies.getByName(RouteStrategyType.Random);

        return submitTask(routeStrategy.route(filterExecutorContexts), taskContext);
    }

    public boolean cancelTask(String taskId) {
        return taskSetManager.cancelTask(taskId);
    }
}
