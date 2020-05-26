package org.kin.scheduler.core.worker.transport;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class WorkerInfo implements Serializable {
    private static final long serialVersionUID = -4507523138506740303L;

    private String workerId;
    private String address;
    private int maxCpuCore;
    private long maxMemory;

    public WorkerInfo() {
    }

    public WorkerInfo(String workerId, String address, int maxCpuCore, long maxMemory) {
        this.workerId = workerId;
        this.address = address;
        this.maxCpuCore = maxCpuCore;
        this.maxMemory = maxMemory;
    }

    //setter && getter

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getMaxCpuCore() {
        return maxCpuCore;
    }

    public void setMaxCpuCore(int maxCpuCore) {
        this.maxCpuCore = maxCpuCore;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(long maxMemory) {
        this.maxMemory = maxMemory;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkerInfo that = (WorkerInfo) o;
        return Objects.equals(workerId, that.workerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workerId);
    }
}
