package org.kin.scheduler.core.domain;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class WorkerResource {
    private String workerId;
    private int cpuCore;

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
}
