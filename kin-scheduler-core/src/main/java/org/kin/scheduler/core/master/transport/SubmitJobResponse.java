package org.kin.scheduler.core.master.transport;

import org.kin.scheduler.core.driver.Job;
import org.kin.scheduler.core.transport.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class SubmitJobResponse extends RPCResult {
    private Job job;

    public SubmitJobResponse() {
    }

    public SubmitJobResponse(boolean success, String desc, Job job) {
        super(success, desc);
        this.job = job;
    }

    //-------------------------------------------------------

    public static SubmitJobResponse success(Job job) {
        return new SubmitJobResponse(true, "", job);
    }

    public static SubmitJobResponse failure(String desc) {
        return new SubmitJobResponse(false, desc, null);
    }

    //setter && getter

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }
}
