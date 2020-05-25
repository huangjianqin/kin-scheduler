package org.kin.scheduler.core.worker.transport;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class WorkerRegisterInfo implements Serializable {
    private static final long serialVersionUID = -9019345657992463377L;

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
