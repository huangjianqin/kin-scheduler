package org.kin.scheduler.core.master.domain;

import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.worker.transport.WorkerInfo;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class WorkerContext {
    private WorkerInfo workerInfo;
    private RpcEndpointRef ref;
    private WorkerResource resource;
    private volatile long lastHeartbeatTime;

    public WorkerContext(WorkerInfo workerInfo, RpcEndpointRef ref) {
        this.workerInfo = workerInfo;
        this.ref = ref;
        this.resource = new WorkerResource(workerInfo.getWorkerId(), workerInfo.getMaxCpuCore());
    }

    public WorkerInfo getWorkerInfo() {
        return workerInfo;
    }

    public RpcEndpointRef ref() {
        return ref;
    }

    public WorkerResource getResource() {
        return resource;
    }

    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(long lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkerContext that = (WorkerContext) o;
        return Objects.equals(workerInfo, that.workerInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workerInfo);
    }
}
