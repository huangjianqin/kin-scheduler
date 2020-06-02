package org.kin.scheduler.admin.core;

import org.kin.scheduler.admin.domain.Constants;
import org.kin.scheduler.admin.domain.TaskType;
import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.driver.route.RouteStrategies;
import org.kin.scheduler.core.driver.route.RouteStrategy;
import org.kin.scheduler.core.driver.scheduler.TaskContext;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.driver.scheduler.TaskScheduler;
import org.kin.scheduler.core.executor.transport.TaskExecLog;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.TaskExecStrategy;
import org.kin.scheduler.core.worker.ExecutorContext;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-03-10
 */
public class KinTaskScheduler extends TaskScheduler<TaskInfoDTO> {
    public KinTaskScheduler(Application app) {
        super(app);
    }

    @Override
    public <R extends Serializable> TaskExecFuture<R> submitTask(TaskInfoDTO dto) {
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

        TaskDescription taskDescription = new TaskDescription(String.valueOf(dto.getJobId()), String.valueOf(dto.getTaskId()));
        taskDescription.setTimeout(dto.getExecTimeout());
        taskDescription.setExecStrategy(TaskExecStrategy.getByName(dto.getExecStrategy()));
        taskDescription.setParam(TaskType.getByName(dto.getType()).parseParam(dto.getParam()));
        taskDescription.setLogFileName(String.valueOf(taskLog.getId()));

        TaskContext taskContext = taskSetManager.init(Collections.singletonList(taskDescription)).get(0);

        //过滤掉已经执行过该task的executor
        List<ExecutorContext> filterExecutorContexts = getAvailableExecutors().stream()
                .filter(ec -> !taskContext.getExecedExecutorIds().contains(ec.getExecutorId()))
                .collect(Collectors.toList());

        RouteStrategy routeStrategy = RouteStrategies.getByName(dto.getRouteStrategy());
        ExecutorContext selected = routeStrategy.route(filterExecutorContexts);

        if (Objects.nonNull(selected)) {
            TaskExecFuture<R> future = submitTask(selected, taskContext);

            taskLog.setExecutorAddress(selected.getExecutorAddress());
            if (Objects.nonNull(future)) {
                //future不为null就是成功调度了
                taskLog.setTriggerCode(Constants.SUCCESS_CODE);
                taskLog.setLogPath(future.getTaskSubmitResult().getLogPath());
                taskLog.setOutputPath(future.getTaskSubmitResult().getOutputPath());
            } else {
                taskLog.setTriggerCode(Constants.FAIL_CODE);
            }
            KinSchedulerContext.instance().getTaskLogDao().updateTriggerInfo(taskLog);

            return future;
        }
        //任务提交失败
        TaskTrigger.instance().submitTaskFail(taskLog);
        return null;
    }

    public TaskExecLog readLog(int logId, int fromLineNum) {
        TaskLog taskLog = KinSchedulerContext.instance().getTaskLogDao().load(logId);
        ExecutorContext target = null;
        for (ExecutorContext executorContext : getAvailableExecutors()) {
            if (executorContext.getExecutorAddress().equals(taskLog.getExecutorAddress())) {
                target = executorContext;
                break;
            }
        }

        if (Objects.nonNull(target)) {
            return target.readLog(taskLog.getLogPath(), fromLineNum);
        }

        return null;
    }
}