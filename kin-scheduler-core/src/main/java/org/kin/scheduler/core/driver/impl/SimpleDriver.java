package org.kin.scheduler.core.driver.impl;

import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.driver.Driver;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.driver.scheduler.impl.SimpleTaskScheduler;
import org.kin.scheduler.core.task.TaskDescription;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-05-16
 */
public class SimpleDriver extends Driver {
    public SimpleDriver(Application app) {
        super(app, new SimpleTaskScheduler(app));
    }

    public <R extends Serializable, PARAM extends Serializable> TaskExecFuture<R> submitTask(TaskDescription<PARAM> taskDescription) {
        return taskScheduler.submitTask(taskDescription);
    }

    public boolean cancelTask(String taskId) {
        return taskScheduler.cancelTask(taskId);
    }
}
