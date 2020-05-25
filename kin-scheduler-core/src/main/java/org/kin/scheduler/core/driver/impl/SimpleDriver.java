package org.kin.scheduler.core.driver.impl;

import org.kin.scheduler.core.driver.Driver;
import org.kin.scheduler.core.driver.SchedulerContext;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.driver.scheduler.impl.SimpleTaskSchedulerImpl;
import org.kin.scheduler.core.task.Task;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-05-16
 */
public class SimpleDriver extends Driver {
    public SimpleDriver(SchedulerContext jobContext) {
        super(jobContext, new SimpleTaskSchedulerImpl(jobContext.getAppName()));
    }

    public <R, PARAM extends Serializable> TaskExecFuture<R> submitTask(Task<PARAM> task) {
        return taskScheduler.submitTask(task);
    }

    public boolean cancelTask(String taskId) {
        return taskScheduler.cancelTask(taskId);
    }
}
