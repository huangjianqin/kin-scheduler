package org.kin.scheduler.core.master.domain;

import org.kin.scheduler.core.domain.WorkerResource;

import java.io.Serializable;
import java.util.Objects;

/**
 * executor资源
 *
 * @author huangjianqin
 * @date 2020-02-13
 */
public class ExecutorResource implements Serializable {
    private static final long serialVersionUID = -1497899582734658387L;
    /** executor id */
    private String executorId;
    /** 占用的worker资源 */
    private WorkerResource workerResource;

    public static ExecutorResource of(String executorId, WorkerResource workerResource) {
        ExecutorResource res = new ExecutorResource();
        res.executorId = executorId;
        res.workerResource = workerResource;
        return res;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutorResource that = (ExecutorResource) o;
        return Objects.equals(executorId, that.executorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executorId);
    }
}
