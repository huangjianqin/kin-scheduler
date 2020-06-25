package org.kin.scheduler.admin.core;

import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.scheduler.admin.core.domain.TaskInfoDTO;
import org.kin.scheduler.admin.domain.TaskType;
import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.driver.ExecutorContext;
import org.kin.scheduler.core.driver.route.RouteStrategies;
import org.kin.scheduler.core.driver.route.RouteStrategy;
import org.kin.scheduler.core.driver.scheduler.TaskContext;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.driver.scheduler.TaskScheduler;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.TaskExecStrategy;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 自定义scheduler, 增加了一些额外的行为
 *
 * @author huangjianqin
 * @date 2020-03-10
 */
public class KinTaskScheduler extends TaskScheduler<TaskInfoDTO> {
    public KinTaskScheduler(RpcEnv rpcEnv, Application app) {
        super(rpcEnv, app);
    }

    @Override
    public <R extends Serializable> TaskExecFuture<R> submitTask(TaskInfoDTO dto) {
        //执行前生成log
        TaskLog taskLog = new TaskLog();
        taskLog.setTaskId(dto.getTaskId());
        taskLog.setJobId(dto.getJobId());
        taskLog.setDesc(dto.getDesc());
        taskLog.setType(dto.getType());
        taskLog.setParam(dto.getParam());
        taskLog.setExecStrategy(dto.getExecStrategy());
        taskLog.setRouteStrategy(dto.getRouteStrategy());
        taskLog.setExecTimeout(dto.getExecTimeout());
        taskLog.setRetryTimes(dto.getNowRetryTimes());
        if (dto.getNowRetryTimes() == 0) {
            //第一次调度
            taskLog.setRetryTimesLimit(dto.getRetryTimesLimit());
        }
        taskLog.setTriggerTime(new Date());
        KinSchedulerContext.instance().getTaskLogDao().save(taskLog);

        //转换成通用的task描述
        TaskDescription taskDescription = TaskDescription.of(String.valueOf(dto.getJobId()), String.valueOf(dto.getTaskId()));
        taskDescription.setTimeout(dto.getExecTimeout());
        taskDescription.setExecStrategy(TaskExecStrategy.getByName(dto.getExecStrategy()));
        taskDescription.setParam(TaskType.getByName(dto.getType()).parseParam(dto.getParam()));
        taskDescription.setLogFileName(String.valueOf(taskLog.getId()));

        //初始化task上下文
        TaskContext taskContext = taskSetManager.init(taskDescription);

        //过滤掉已经执行过该task的executor
        List<ExecutorContext> filterExecutorContexts = getAvailableExecutors().stream()
                .filter(ec -> !taskContext.getExecedExecutorIds().contains(ec.getExecutorId()))
                .collect(Collectors.toList());

        RouteStrategy routeStrategy = RouteStrategies.getByName(dto.getRouteStrategy());
        //根据路由策略选择出合适的executor
        ExecutorContext selected = routeStrategy.route(filterExecutorContexts);

        if (Objects.nonNull(selected)) {
            TaskExecFuture<R> future = submitTask(selected, taskContext);

            taskLog.setExecutorAddress(selected.getExecutorAddress());
            taskLog.setWorkerId(selected.getWorkerId());
            if (Objects.nonNull(future)) {
                //future不为null就是成功调度了
                taskLog.setTriggerCode(TaskLog.SUCCESS);
                taskLog.setLogPath(future.getTaskSubmitResp().getLogPath());
                taskLog.setOutputPath(future.getTaskSubmitResp().getOutputPath());

                KinSchedulerContext.instance().getTaskLogDao().updateTriggerInfo(taskLog);
            } else {
                //任务提交失败
                TaskTrigger.instance().submitTaskFail(taskLog);

                KinSchedulerContext.instance().getTaskLogDao().updateTriggerInfo(taskLog);
                return null;
            }

            return future;
        }
        //任务提交失败
        TaskTrigger.instance().submitTaskFail(taskLog);
        return null;
    }
}
