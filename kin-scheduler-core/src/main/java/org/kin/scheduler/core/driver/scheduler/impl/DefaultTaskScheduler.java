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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认实现的TaskScheduler
 * 支持原生@{link TaskDescription} 的提交task方式
 *
 * @author huangjianqin
 * @date 2020-03-09
 */
public class DefaultTaskScheduler extends TaskScheduler<TaskDescription> {
    /** 任务计数器 */
    private AtomicInteger taskIdCounter = new AtomicInteger(1);

    public DefaultTaskScheduler(RpcEnv rpcEnv, Application app) {
        super(rpcEnv, app);
    }

    /**
     * 分配task id
     */
    private void assignTaskId(TaskDescription taskDescription) {
        //task id = [job Id]-Task-[counter]
        taskDescription.setTaskId(taskDescription.getJobId().concat("-Task-").concat(String.valueOf(taskIdCounter.getAndIncrement())));
    }

    @Override
    public <R extends Serializable> TaskExecFuture<R> submitTask(TaskDescription taskDescription) {
        assignTaskId(taskDescription);
        //初始化TaskContext 用于任务状态追踪
        TaskContext taskContext = taskSetManager.init(taskDescription);
        //默认使用随机的executor路由策略选择executor
        ExecutorContext selected = getAvailableExecutors(taskContext, RouteStrategies.getByName(RouteStrategyType.Random));
        if (Objects.nonNull(selected)) {
            //提交task
            return submitTask(selected, taskContext);
        }
        return null;
    }
}
