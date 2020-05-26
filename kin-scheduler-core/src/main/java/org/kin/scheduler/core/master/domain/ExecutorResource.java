package org.kin.scheduler.core.master.domain;

import org.kin.scheduler.core.domain.WorkerResource;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class ExecutorResource implements Serializable {
    private static final long serialVersionUID = -1497899582734658387L;

    private String executorId;
    private WorkerResource workerResource;

    public ExecutorResource() {
    }

    public ExecutorResource(String executorId, WorkerResource workerResource) {
        this.executorId = executorId;
        this.workerResource = workerResource;
    }

    //setter && getter
    public String getExecutorId() {
        return executorId;
    }

    public void setExecutorId(String executorId) {
        this.executorId = executorId;
    }

    public WorkerResource getWorkerResource() {
        return workerResource;
    }

    public void setWorkerResource(WorkerResource workerResource) {
        this.workerResource = workerResource;
    }
}
