package org.kin.scheduler.core.driver;

import org.kin.scheduler.core.master.executor.AllocateStrategyType;

/**
 * @author huangjianqin
 * @date 2020-02-11
 */
public abstract class SchedulerContext {
    private String appName;
    /** master rpc接口 */
    private String masterAddress;
    /** executor分配策略 */
    private AllocateStrategyType allocateStrategyType = AllocateStrategyType.Hash;
    /** driver rpc服务端口 */
    private int driverPort = 50000;

    public SchedulerContext() {
    }

    public SchedulerContext(String appName) {
        this.appName = appName;
    }

    //-----------------------------------------------------------------------------------------------

    public SchedulerContext appName(String appName) {
        this.appName = appName;
        return this;
    }

    public SchedulerContext master(String masterAddress) {
        this.masterAddress = masterAddress;
        return this;
    }

    public SchedulerContext master(String masterAddress, AllocateStrategyType allocateStrategyType) {
        master(masterAddress);
        return allocateStrategy(allocateStrategyType);
    }

    public SchedulerContext driverPort(int driverPort) {
        this.driverPort = driverPort;
        return this;
    }

    public SchedulerContext allocateStrategy(AllocateStrategyType allocateStrategyType) {
        this.allocateStrategyType = allocateStrategyType;
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
}
