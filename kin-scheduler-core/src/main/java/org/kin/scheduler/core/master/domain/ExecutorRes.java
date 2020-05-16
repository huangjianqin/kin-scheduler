package org.kin.scheduler.core.master.domain;

import org.kin.scheduler.core.domain.WorkerRes;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class ExecutorRes {
    private String executorId;
    private WorkerRes workerRes;

    public ExecutorRes(String executorId, WorkerRes workerRes) {
        this.executorId = executorId;
        this.workerRes = workerRes;
    }

    //getter

    public String getExecutorId() {
        return executorId;
    }

    public WorkerRes getWorkerRes() {
        return workerRes;
    }
}
