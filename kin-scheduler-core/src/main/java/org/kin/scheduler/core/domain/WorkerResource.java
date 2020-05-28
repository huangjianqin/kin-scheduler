package org.kin.scheduler.core.domain;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class WorkerResource implements Serializable {
    private static final long serialVersionUID = -9114436364954662176L;

    private String workerId;
    private int cpuCore;

    public WorkerResource() {
    }

    public WorkerResource(String workerId, int cpuCore) {
        this.workerId = workerId;
        this.cpuCore = cpuCore;
    }

    public void useCpuCore(int cpuCore) {
        this.cpuCore -= cpuCore;
    }

    public void recoverCpuCore(int cpuCore) {
        this.cpuCore += cpuCore;
    }

    public boolean hasEnoughCpuCore(int cpuCore) {
        return this.cpuCore >= cpuCore;
    }

    //getter
    public String getWorkerId() {
        return workerId;
    }

    public int getCpuCore() {
        return cpuCore;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public void setCpuCore(int cpuCore) {
        this.cpuCore = cpuCore;
    }
}
