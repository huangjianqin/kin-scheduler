package org.kin.scheduler.admin.core;

import org.kin.scheduler.core.driver.Driver;
import org.kin.scheduler.core.driver.SchedulerContext;
import org.kin.scheduler.core.driver.TaskExecFuture;
import org.kin.scheduler.core.executor.domain.TaskExecLog;
import org.kin.scheduler.core.master.DriverMasterBackend;

/**
 * @author huangjianqin
 * @date 2020-03-10
 */
public class KinDriver extends Driver<KinTaskSchedulerImpl> {
    public KinDriver(SchedulerContext jobContext, DriverMasterBackend driverMasterBackend) {
        super(jobContext, KinTaskSchedulerImpl::new);
        super.driverMasterBackend = driverMasterBackend;
    }

    public <R> TaskExecFuture<R> submitTask(TaskInfoDTO dto) {
        return taskScheduler.submitTask(dto);
    }

    public boolean cancelTask(String taskId) {
        return taskScheduler.cancelTask(taskId);
    }

    public TaskExecLog readLog(int logId, int fromLineNum) {
        return taskScheduler.readLog(logId, fromLineNum);
    }
}
