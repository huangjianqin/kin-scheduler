package org.kin.scheduler.admin.core;

import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.driver.Driver;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.executor.log.TaskExecLog;
import org.kin.scheduler.core.master.DriverMasterBackend;

/**
 * @author huangjianqin
 * @date 2020-03-10
 */
public class KinDriver extends Driver {
    public KinDriver(Application app, DriverMasterBackend driverMasterBackend) {
        super(app, new KinTaskScheduler(app));
        super.driverMasterBackend = driverMasterBackend;
    }

    public <R> TaskExecFuture<R> submitTask(TaskInfoDTO dto) {
        return taskScheduler.submitTask(dto);
    }

    public boolean cancelTask(String taskId) {
        return taskScheduler.cancelTask(taskId);
    }

    public TaskExecLog readLog(int logId, int fromLineNum) {
        return ((KinTaskScheduler) taskScheduler).readLog(logId, fromLineNum);
    }
}
