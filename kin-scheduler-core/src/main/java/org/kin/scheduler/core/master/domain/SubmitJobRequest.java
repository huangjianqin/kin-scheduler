package org.kin.scheduler.core.master.domain;

import org.kin.scheduler.core.master.executor.AllocateStrategyType;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-11
 */
public class SubmitJobRequest implements Serializable {
    private String appName;
    private String allocateStrategy;

    public SubmitJobRequest() {
    }

    public static SubmitJobRequest create(String appName, AllocateStrategyType allocateStrategy) {
        SubmitJobRequest request = new SubmitJobRequest();
        request.setAppName(appName);
        request.setAllocateStrategy(allocateStrategy.name());
        return request;
    }

    //setter && getter

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAllocateStrategy() {
        return allocateStrategy;
    }

    public void setAllocateStrategy(String allocateStrategy) {
        this.allocateStrategy = allocateStrategy;
    }
}
