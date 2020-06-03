package org.kin.scheduler.core.driver;

import org.kin.scheduler.core.master.executor.allocate.AllocateStrategies;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategyType;

import java.io.Serializable;
import java.util.Objects;

/**
 * 应用配置
 *
 * @author huangjianqin
 * @date 2020-05-26
 */
public class ApplicationDescription implements Serializable {
    private static final long serialVersionUID = 6154305000643413958L;

    /** 应用名 */
    private String appName;
    /** 资源分配策略 */
    private AllocateStrategyType allocateStrategyType;
    //需要cpu核心数
    private int cpuCoreNum;
    //每个executor最小需要cpu核心数
    private int minCoresPerExecutor;
    //每个worker一个Executor
    private boolean oneExecutorPerWorker;


    public AllocateStrategy getAllocateStrategy() {
        return AllocateStrategies.getByName(allocateStrategyType);
    }

    //setter && getter
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public AllocateStrategyType getAllocateStrategyType() {
        return allocateStrategyType;
    }

    public void setAllocateStrategyType(AllocateStrategyType allocateStrategyType) {
        this.allocateStrategyType = allocateStrategyType;
    }

    public int getCpuCoreNum() {
        return cpuCoreNum;
    }

    public void setCpuCoreNum(int cpuCoreNum) {
        this.cpuCoreNum = cpuCoreNum;
    }

    public int getMinCoresPerExecutor() {
        return minCoresPerExecutor;
    }

    public void setMinCoresPerExecutor(int minCoresPerExecutor) {
        this.minCoresPerExecutor = minCoresPerExecutor;
    }

    public boolean isOneExecutorPerWorker() {
        return oneExecutorPerWorker;
    }

    public void setOneExecutorPerWorker(boolean oneExecutorPerWorker) {
        this.oneExecutorPerWorker = oneExecutorPerWorker;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApplicationDescription that = (ApplicationDescription) o;
        return Objects.equals(appName, that.appName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName);
    }

    @Override
    public String toString() {
        return "ApplicationDescription{" +
                "appName='" + appName + '\'' +
                ", allocateStrategyType=" + allocateStrategyType +
                ", cpuCoreNum=" + cpuCoreNum +
                ", minCoresPerExecutor=" + minCoresPerExecutor +
                ", oneExecutorPerWorker=" + oneExecutorPerWorker +
                '}';
    }
}
