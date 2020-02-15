package org.kin.scheduler.core.master;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class JobRes {
    private String jobId;
    private List<ExecutorRes> useExecutorReses;

    public JobRes(String jobId, List<ExecutorRes> useExecutorReses) {
        this.jobId = jobId;
        this.useExecutorReses = useExecutorReses;
    }

    //getter
    public String getJobId() {
        return jobId;
    }

    public List<ExecutorRes> getUseExecutorReses() {
        return useExecutorReses;
    }
}
