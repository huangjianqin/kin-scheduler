package org.kin.scheduler.core.driver;

import java.util.Map;

/**
 * @author huangjianqin
 * @date 2020-02-10
 */
public class Job {
    private String jobId;
    private Map<String, String> availableExecutors;

    public Job() {
    }

    public Job(String jobId, Map<String, String> availableExecutors) {
        this.jobId = jobId;
        this.availableExecutors = availableExecutors;
    }

    //setter && getter

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Map<String, String> getAvailableExecutors() {
        return availableExecutors;
    }

    public void setAvailableExecutors(Map<String, String> availableExecutors) {
        this.availableExecutors = availableExecutors;
    }
}
