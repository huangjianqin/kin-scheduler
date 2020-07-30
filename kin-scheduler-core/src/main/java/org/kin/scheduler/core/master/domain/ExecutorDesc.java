package org.kin.scheduler.core.master.domain;

import java.util.Objects;

/**
 * executor资源
 *
 * @author huangjianqin
 * @date 2020-02-13
 */
public class ExecutorDesc {
    /** executor id */
    private String executorId;
    /** 所属worker */
    private WorkerContext worker;
    /** 占用cpu核心数 */
    private int usedCpuCore;
    /** 占用内存 */
    private int usedMemory;

    public static ExecutorDesc of(String executorId, WorkerContext worker, int usedCpuCore, int usedMemory) {
        ExecutorDesc res = new ExecutorDesc();
        res.executorId = executorId;
        res.worker = worker;
        res.usedCpuCore = usedCpuCore;
        res.usedMemory = usedMemory;
        return res;
    }

    /**
     * 释放占用资源
     */
    public void releaseResources() {
        worker.recoverCpuCore(usedCpuCore);
        worker.recoverMemory(usedMemory);
    }

    //setter && getter
    public String getExecutorId() {
        return executorId;
    }

    public WorkerContext getWorker() {
        return worker;
    }

    public int getUsedCpuCore() {
        return usedCpuCore;
    }

    public int getUsedMemory() {
        return usedMemory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExecutorDesc that = (ExecutorDesc) o;
        return Objects.equals(executorId, that.executorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executorId);
    }
}
