package org.kin.scheduler.core.worker.transport;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class WorkerInfo {
    private String workerId;
    private String address;

    private long usedMemory;
    private long maxMemory;

    public WorkerInfo() {
    }

    public WorkerInfo(String workerId, String address, long usedMemory, long maxMemory) {
        this.workerId = workerId;
        this.address = address;
        this.usedMemory = usedMemory;
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

    public long getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(long usedMemory) {
        this.usedMemory = usedMemory;
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
