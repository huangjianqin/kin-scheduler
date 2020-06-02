package org.kin.scheduler.core.driver;

import org.kin.framework.utils.SysUtils;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategyType;

/**
 * @author huangjianqin
 * @date 2020-02-11
 */
public abstract class Application {
    private String appName;
    /** master rpc接口 */
    private String masterAddress = "0.0.0.0:46668";
    /** executor分配策略 */
    private AllocateStrategyType allocateStrategyType = AllocateStrategyType.Hash;
    /** driver rpc服务端口 */
    private int driverPort = 46000;
    //需要cpu核心数
    private int cpuCoreNum = SysUtils.CPU_NUM;
    //每个executor最小需要cpu核心数
    private int minCoresPerExecutor = SysUtils.CPU_NUM;
    //每个worker一个Executor
    private boolean oneExecutorPerWorker;

    public Application() {
    }

    public Application(String appName) {
        this.appName = appName;
    }

    //-----------------------------------------------------------------------------------------------

    public Application appName(String appName) {
        this.appName = appName;
        return this;
    }

    public Application master(String masterAddress) {
        this.masterAddress = masterAddress;
        return this;
    }

    public Application master(String masterAddress, AllocateStrategyType allocateStrategyType) {
        master(masterAddress);
        return allocateStrategy(allocateStrategyType);
    }

    public Application driverPort(int driverPort) {
        this.driverPort = driverPort;
        return this;
    }

    public Application allocateStrategy(AllocateStrategyType allocateStrategyType) {
        this.allocateStrategyType = allocateStrategyType;
        return this;
    }

    public Application cpuCore(int cpuCoreNum) {
        this.cpuCoreNum = cpuCoreNum;
        return this;
    }

    public Application minCoresPerExecutor(int minCoresPerExecutor) {
        this.minCoresPerExecutor = minCoresPerExecutor;
        return this;
    }

    public Application oneExecutorPerWorker(boolean oneExecutorPerWorker) {
        this.oneExecutorPerWorker = oneExecutorPerWorker;
        return this;
    }

    //getter
    public String getAppName() {
        return appName;
    }

    public String getMasterAddress() {
        return masterAddress;
    }

    public AllocateStrategyType getAllocateStrategyType() {
        return allocateStrategyType;
    }

    public int getDriverPort() {
        return driverPort;
    }

    public int getCpuCoreNum() {
        return cpuCoreNum;
    }

    public int getMinCoresPerExecutor() {
        return minCoresPerExecutor;
    }

    public boolean isOneExecutorPerWorker() {
        return oneExecutorPerWorker;
    }
}