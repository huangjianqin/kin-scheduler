package org.kin.scheduler.core.master;

import org.kin.scheduler.core.master.domain.SubmitJobRequest;
import org.kin.scheduler.core.master.domain.SubmitJobResponse;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public interface DriverMasterBackend {
    SubmitJobResponse submitJob(SubmitJobRequest request);

    void jonFinish(String jobId);
}
