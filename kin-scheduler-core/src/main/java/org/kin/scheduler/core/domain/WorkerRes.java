package org.kin.scheduler.core.domain;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class WorkerRes {
    private String workerId;

    public WorkerRes(String workerId) {
        this.workerId = workerId;
    }

    //getter

    public String getWorkerId() {
        return workerId;
    }
}
