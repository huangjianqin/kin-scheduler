package org.kin.scheduler.core.master.domain;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-11
 */
public class SubmitJobRequest implements Serializable {
    private String appName;
    private int parallelism;

    public SubmitJobRequest() {
    }

    public static SubmitJobRequest create(String appName, int parallelism) {
        SubmitJobRequest request = new SubmitJobRequest();
        request.setAppName(appName);
        request.setParallelism(parallelism);
        return request;
    }

    //setter && getter

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }
}
