package org.kin.scheduler.core.driver;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-10
 */
public class Job implements Serializable {
    private String jobId;

    public Job() {
    }

    public Job(String jobId) {
        this.jobId = jobId;
    }

    //setter && getter
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
