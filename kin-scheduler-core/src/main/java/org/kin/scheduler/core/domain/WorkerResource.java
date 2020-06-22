package org.kin.scheduler.core.domain;

import java.io.Serializable;

/**
 * worker资源
 *
 * @author huangjianqin
 * @date 2020-02-13
 */
public class WorkerResource implements Serializable {
    private static final long serialVersionUID = -9114436364954662176L;
    /** worker id */
    private String workerId;
    /** worker占用cpu核心数 */
    private int cpuCore;

    public static WorkerResource of(String workerId, int cpuCore) {
        WorkerResource res = new WorkerResource();
        res.workerId = workerId;
        res.cpuCore = cpuCore;
        return res;
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

    //setter && getter
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
