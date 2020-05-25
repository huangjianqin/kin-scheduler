package org.kin.scheduler.core.master.domain;

import org.kin.framework.service.AbstractService;
import org.kin.scheduler.core.domain.WorkerRes;
import org.kin.scheduler.core.worker.transport.WorkerInfo;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class WorkerContext extends AbstractService {
    private WorkerInfo workerInfo;
    private WorkerRes res;
    private volatile long lastHeartbeatTime;

    public WorkerContext(WorkerInfo workerInfo) {
        super(workerInfo.getWorkerId());
        this.workerInfo = workerInfo;
        this.res = new WorkerRes(workerInfo.getWorkerId());
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    public WorkerInfo getWorkerInfo() {
        return workerInfo;
    }

    public WorkerRes getRes() {
        return res;
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
