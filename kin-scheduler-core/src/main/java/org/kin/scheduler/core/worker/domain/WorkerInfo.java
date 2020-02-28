package org.kin.scheduler.core.worker.domain;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class WorkerInfo {
    private String workerId;
    private String address;

    private long usedMemory;
    private long maxMemory;
    private int uesdParallelism;
    private int maxParallelism;

    public WorkerInfo() {
    }

    public WorkerInfo(String workerId, String address, long usedMemory, long maxMemory, int uesdParallelism, int maxParallelism) {
        this.workerId = workerId;
        this.address = address;
        this.usedMemory = usedMemory;
        this.maxMemory = maxMemory;
        this.uesdParallelism = uesdParallelism;
        this.maxParallelism = maxParallelism;
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

    public int getUesdParallelism() {
        return uesdParallelism;
    }

    public void setUesdParallelism(int uesdParallelism) {
        this.uesdParallelism = uesdParallelism;
    }

    public int getMaxParallelism() {
        return maxParallelism;
    }

    public void setMaxParallelism(int maxParallelism) {
        this.maxParallelism = maxParallelism;
    }
}
