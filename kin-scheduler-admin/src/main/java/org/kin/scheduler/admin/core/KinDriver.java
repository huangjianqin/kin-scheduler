package org.kin.scheduler.admin.core;

import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.driver.Driver;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.master.DriverMasterBackend;
import org.kin.scheduler.core.worker.transport.TaskExecFileContent;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-03-10
 */
public class KinDriver extends Driver {
    public KinDriver(Application app, DriverMasterBackend driverMasterBackend) {
        super(app, new KinTaskScheduler(app));
        super.driverMasterBackend = driverMasterBackend;
    }

    public <R extends Serializable> TaskExecFuture<R> submitTask(TaskInfoDTO dto) {
        return taskScheduler.submitTask(dto);
    }

    public boolean cancelTask(String taskId) {
        return taskScheduler.cancelTask(taskId);
    }

    public TaskExecFileContent readLog(int logId, int fromLineNum) {
        TaskLog taskLog = KinSchedulerContext.instance().getTaskLogDao().load(logId);
        if (Objects.nonNull(taskLog)) {
            return driverMasterBackend.readFile(taskLog.getWorkerId(), taskLog.getLogPath(), fromLineNum);
        }

        return null;
    }

    public TaskExecFileContent readOutput(int logId, int fromLineNum) {
        TaskLog taskLog = KinSchedulerContext.instance().getTaskLogDao().load(logId);
        if (Objects.nonNull(taskLog)) {
            return driverMasterBackend.readFile(taskLog.getWorkerId(), taskLog.getOutputPath(), fromLineNum);
        }

        return null;
    }
}
