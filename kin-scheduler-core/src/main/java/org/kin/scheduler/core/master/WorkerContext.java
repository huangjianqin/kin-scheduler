package org.kin.scheduler.core.master;

import org.kin.framework.service.AbstractService;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.scheduler.core.worker.WorkerBackend;
import org.kin.scheduler.core.worker.domain.WorkerInfo;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class WorkerContext extends AbstractService {
    private WorkerInfo workerInfo;
    private ReferenceConfig<WorkerBackend> workerBackendReferenceConfig;
    private WorkerBackend workerBackend;
    private WorkerRes res;

    public WorkerContext(WorkerInfo workerInfo) {
        super(WorkerContext.class.getSimpleName().concat(workerInfo.getWorkerId()));
        this.workerInfo = workerInfo;
        this.res = new WorkerRes(workerInfo.getWorkerId());
        this.res.recoverParallelismRes(workerInfo.getUesdParallelism());
    }

    @Override
    public void start() {
        super.start();
        workerBackendReferenceConfig = References.reference(WorkerBackend.class)
                .appName(getName())
                .urls(workerInfo.getAddress());
        workerBackend = workerBackendReferenceConfig.get();
    }

    @Override
    public void close() {
        super.close();
        workerBackendReferenceConfig.disable();
    }

    //getter
    public WorkerInfo getWorkerInfo() {
        return workerInfo;
    }

    public WorkerBackend getWorkerBackend() {
        return workerBackend;
    }

    public WorkerRes getRes() {
        return res;
    }
}
