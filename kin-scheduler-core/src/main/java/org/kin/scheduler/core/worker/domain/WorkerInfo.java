package org.kin.scheduler.core.worker.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * worker信息
 *
 * @author huangjianqin
 * @date 2020-02-09
 */
public class WorkerInfo implements Serializable {
    private static final long serialVersionUID = -4507523138506740303L;
    /** worker id */
    private String workerId;
    /** worker占用核心数 */
    private int maxCpuCore;
    /** worker占用内存数 */
    private long maxMemory;

    public static WorkerInfo of(String workerId, int maxCpuCore, long maxMemory) {
        WorkerInfo workerInfo = new WorkerInfo();
        workerInfo.workerId = workerId;
        workerInfo.maxCpuCore = maxCpuCore;
        workerInfo.maxMemory = maxMemory;
        return workerInfo;
    }

    //setter && getter

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
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
