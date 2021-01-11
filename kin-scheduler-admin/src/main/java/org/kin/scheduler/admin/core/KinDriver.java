package org.kin.scheduler.admin.core;

import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.scheduler.admin.core.domain.TaskInfoDTO;
import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.driver.Driver;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.worker.transport.TaskExecFileContent;

import java.io.Serializable;
import java.util.Objects;

/**
 * 自定义Driver
 *
 * @author huangjianqin
 * @date 2020-03-10
 */
public class KinDriver extends Driver {
    public KinDriver(RpcEnv rpcEnv, Application app) {
        super(rpcEnv, app, new KinTaskScheduler(rpcEnv, app));
    }

    /**
     * 提交task
     */
    public <R extends Serializable> TaskExecFuture<R> submitTask(TaskInfoDTO dto) {
        return taskScheduler.submitTask(dto);
    }

    /**
     * 读取task log输出
     *
     * @param logId       task log id
     * @param fromLineNum 开始行数
     * @return log内容
     */
    public TaskExecFileContent readLog(int logId, int fromLineNum) {
        TaskLog taskLog = KinSchedulerContext.instance().getTaskLogDao().selectById(logId);
        if (Objects.nonNull(taskLog)) {
            return readFile(taskLog.getWorkerId(), taskLog.getLogPath(), fromLineNum);
        }

        return null;
    }

    /**
     * 读取task output输出
     * @param logId task log id
     * @param fromLineNum 开始行数
     * @return output内容
     */
    public TaskExecFileContent readOutput(int logId, int fromLineNum) {
        TaskLog taskLog = KinSchedulerContext.instance().getTaskLogDao().selectById(logId);
        if (Objects.nonNull(taskLog)) {
            return readFile(taskLog.getWorkerId(), taskLog.getOutputPath(), fromLineNum);
        }

        return null;
    }
}
