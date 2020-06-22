package org.kin.scheduler.core.master.domain;

import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.worker.domain.WorkerInfo;

import java.util.Objects;

/**
 * worker上下文
 *
 * @author huangjianqin
 * @date 2020-02-09
 */
public class WorkerContext {
    /** worker 信息 */
    private WorkerInfo workerInfo;
    /** worker client ref */
    private RpcEndpointRef ref;
    /** worker资源 */
    private WorkerResource resource;
    /** 上次worker心跳时间 */
    private volatile long lastHeartbeatTime;

    public WorkerContext(WorkerInfo workerInfo, RpcEndpointRef ref) {
        this.workerInfo = workerInfo;
        this.ref = ref;
        this.resource = WorkerResource.of(workerInfo.getWorkerId(), workerInfo.getMaxCpuCore());
    }

    //----------------------------------------------------------------------------------------------------------------------------------
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
