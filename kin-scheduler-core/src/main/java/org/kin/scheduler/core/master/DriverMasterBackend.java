package org.kin.scheduler.core.master;

import org.kin.scheduler.core.master.domain.SubmitJobRequest;
import org.kin.scheduler.core.master.domain.SubmitJobResponse;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public interface DriverMasterBackend {
    /**
     * 往master提交job, 申请资源
     *
     * @param request 请求
     * @return 返回结果
     */
    SubmitJobResponse submitJob(SubmitJobRequest request);

    /**
     * 告诉masterjob完成, 释放资源
     *
     * @param jobId jobId
     */
    void jonFinish(String jobId);
}
