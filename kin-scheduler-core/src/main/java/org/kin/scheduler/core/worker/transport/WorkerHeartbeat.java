package org.kin.scheduler.core.worker.transport;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-05-22
 */
public class WorkerHeartbeat implements Serializable {
    private static final long serialVersionUID = -5853704074373477394L;
    /** 心跳worker id */
    private String workerId;

    public WorkerHeartbeat() {
    }

    public WorkerHeartbeat(String workerId) {
        this.workerId = workerId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
}
