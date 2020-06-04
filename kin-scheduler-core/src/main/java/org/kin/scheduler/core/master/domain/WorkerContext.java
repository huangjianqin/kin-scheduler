package org.kin.scheduler.core.master.domain;

import org.kin.framework.service.AbstractService;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.master.transport.ExecutorLaunchInfo;
import org.kin.scheduler.core.worker.WorkerBackend;
import org.kin.scheduler.core.worker.transport.ExecutorLaunchResult;
import org.kin.scheduler.core.worker.transport.TaskExecFileContent;
import org.kin.scheduler.core.worker.transport.WorkerInfo;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class WorkerContext extends AbstractService implements WorkerBackend {
    private WorkerInfo workerInfo;
    private ReferenceConfig<WorkerBackend> workerBackendReferenceConfig;
    private WorkerBackend workerBackend;
    private WorkerResource resource;
    private volatile long lastHeartbeatTime;

    public WorkerContext(WorkerInfo workerInfo) {
        super(workerInfo.getWorkerId());
        this.workerInfo = workerInfo;
        this.resource = new WorkerResource(workerInfo.getWorkerId(), workerInfo.getMaxCpuCore());
    }

    @Override
    public void serviceStart() {
        workerBackendReferenceConfig = References.reference(WorkerBackend.class)
                .appName(getName())
                .urls(workerInfo.getAddress());
        workerBackend = workerBackendReferenceConfig.get();
    }

    @Override
    public void serviceStop() {
        super.stop();
        workerBackendReferenceConfig.disable();
    }

    @Override
    public void reconnect2Master() {
        throw new IllegalStateException();
    }

    @Override
    public ExecutorLaunchResult launchExecutor(ExecutorLaunchInfo launchInfo) {
        return workerBackend.launchExecutor(launchInfo);
    }

    @Override
    public TaskExecFileContent readFile(String path, int fromLineNum) {
        return workerBackend.readFile(path, fromLineNum);
    }


    public WorkerInfo getWorkerInfo() {
        return workerInfo;
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
