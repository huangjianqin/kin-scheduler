package org.kin.scheduler.core.driver.scheduler.impl;

import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.driver.ExecutorContext;
import org.kin.scheduler.core.driver.route.RouteStrategies;
import org.kin.scheduler.core.driver.route.RouteStrategyType;
import org.kin.scheduler.core.driver.scheduler.TaskContext;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.driver.scheduler.TaskScheduler;
import org.kin.scheduler.core.task.TaskDescription;

import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public class DefaultTaskScheduler extends TaskScheduler<TaskDescription> {
    private AtomicInteger taskIdCounter = new AtomicInteger(1);

    public DefaultTaskScheduler(RpcEnv rpcEnv, Application app) {
        super(rpcEnv, app);
    }

    private void assignTaskId(TaskDescription taskDescription) {
        taskDescription.setJobId(taskDescription.getJobId());
        taskDescription.setTaskId(taskDescription.getJobId().concat("-Task-").concat(String.valueOf(taskIdCounter.getAndIncrement())));
    }

    @Override
    public <R extends Serializable> TaskExecFuture<R> submitTask(TaskDescription taskDescription) {
        assignTaskId(taskDescription);
        TaskContext taskContext = taskSetManager.init(Collections.singletonList(taskDescription)).get(0);

        ExecutorContext selected = getAvailableExecutors(taskContext, RouteStrategies.getByName(RouteStrategyType.Random));
        if (Objects.nonNull(selected)) {
            return submitTask(selected, taskContext);
        }
        return null;
    }
}
