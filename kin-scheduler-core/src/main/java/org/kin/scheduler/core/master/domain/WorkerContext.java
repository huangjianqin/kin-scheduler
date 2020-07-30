package org.kin.scheduler.core.master.domain;

import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.scheduler.core.worker.domain.WorkerInfo;

import java.util.Objects;

/**
 * worker上下文
 *
 * @author huangjianqin
 * @date 2020-02-09
 */
public class WorkerContext {
    /** worker 信息 */
    private WorkerInfo workerInfo;
    /** worker 状态 */
    private WorkerState state;
    /** worker client ref */
    private RpcEndpointRef ref;
    /** 上次worker心跳时间 */
    private volatile long lastHeartbeatTime;
    /** worker已被占用cpu核心数 */
    private int usedCpuCore;
    /** worker已被占用内存 */
    private int usedMemory;

    public WorkerContext(WorkerInfo workerInfo, RpcEndpointRef ref) {
        this.workerInfo = workerInfo;
        alive();
        this.ref = ref;
    }
    //-------------------------------------------------------------------------------------------------------------------

    /**
     * worker状态
     */
    public enum WorkerState {
        /**
         * worker 有效
         */
        ALIVE,
        /**
         * worker 无效
         */
        DEAD;
    }
    //-------------------------------------------------------------------------------------------------------------------

    /**
     * 使用cpu核心数
     */
    public void useCpuCore(int cpuCore) {
        this.usedCpuCore -= cpuCore;
    }

    /**
     * 回收已使用cpu核心数
     */
    public void recoverCpuCore(int cpuCore) {
        this.usedCpuCore += cpuCore;
    }

    /**
     * 是否有足够cpu核心数
     */
    public boolean hasEnoughCpuCore(int cpuCore) {
        return this.usedCpuCore >= cpuCore;
    }

    /**
     * 占用内存
     */
    public void useMemory(int memory) {
        this.usedMemory -= memory;
    }

    /**
     * 回收已占用内存
     */
    public void recoverMemory(int memory) {
        this.usedMemory += memory;
    }

    /**
     * 是否有足够内存
     */
    public boolean hasEnoughMemory(int memory) {
        return this.usedMemory >= memory;
    }

    /**
     * 是否有资源
     */
    public boolean hasResources() {
        return workerInfo.getMaxCpuCore() - usedCpuCore > 0 && workerInfo.getMaxMemory() - usedMemory > 0;
    }

    /**
     * worker是否有效
     */
    public boolean isAlive() {
        return state == WorkerState.ALIVE;
    }

    /**
     * worker是否无效
     */
    public boolean isDead() {
        return state == WorkerState.DEAD;
    }

    /**
     * 使worker有效
     */
    public void alive() {
        state = WorkerState.ALIVE;
    }

    /**
     * 使worker无效
     */
    public void dead() {
        state = WorkerState.DEAD;
    }

    //----------------------------------------------------------------------------------------------------------------------------------
    public WorkerInfo getWorkerInfo() {
        return workerInfo;
    }

    public RpcEndpointRef ref() {
        return ref;
    }

    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(long lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
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
        WorkerContext that = (WorkerContext) o;
        return Objects.equals(workerInfo, that.workerInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workerInfo);
    }
}
