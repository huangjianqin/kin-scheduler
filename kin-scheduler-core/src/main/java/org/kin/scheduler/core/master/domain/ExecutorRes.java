package org.kin.scheduler.core.master.domain;

import org.kin.scheduler.core.domain.WorkerRes;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class ExecutorRes implements Serializable {
    private static final long serialVersionUID = -1497899582734658387L;

    private String executorId;
    private WorkerRes workerRes;

    public ExecutorRes() {
    }

    public ExecutorRes(String executorId, WorkerRes workerRes) {
        this.executorId = executorId;
        this.workerRes = workerRes;
    }

    //setter && getter
    public String getExecutorId() {
        return executorId;
    }

    public void setExecutorId(String executorId) {
        this.executorId = executorId;
    }

    public WorkerRes getWorkerRes() {
        return workerRes;
    }

    public void setWorkerRes(WorkerRes workerRes) {
        this.workerRes = workerRes;
    }
}
