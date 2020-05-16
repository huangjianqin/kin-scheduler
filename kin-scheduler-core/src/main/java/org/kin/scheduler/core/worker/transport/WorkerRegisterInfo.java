package org.kin.scheduler.core.worker.transport;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class WorkerRegisterInfo {
    private WorkerInfo workerInfo;

    public WorkerRegisterInfo() {
    }

    public WorkerRegisterInfo(WorkerInfo workerInfo) {
        this.workerInfo = workerInfo;
    }

    //setter && getter

    public WorkerInfo getWorkerInfo() {
        return workerInfo;
    }

    public void setWorkerInfo(WorkerInfo workerInfo) {
        this.workerInfo = workerInfo;
    }
}
