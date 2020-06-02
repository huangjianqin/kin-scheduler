package org.kin.scheduler.core.master;

import org.kin.scheduler.core.driver.transport.ApplicationRegisterInfo;
import org.kin.scheduler.core.master.transport.ApplicationRegisterResponse;
import org.kin.scheduler.core.worker.transport.TaskExecFileContent;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public interface DriverMasterBackend {
    /**
     * 往master注册app
     *
     * @param request 请求
     * @return 返回结果
     */
    ApplicationRegisterResponse registerApplication(ApplicationRegisterInfo request);

    /**
     * 请求master分配资源
     *
     * @param request 请求
     * @return 返回结果
     */
    void scheduleResource(String appName);

    /**
     * 告诉master application完成, 释放资源
     *
     * @param appName appName
     */
    void applicationEnd(String appName);

    /**
     * 从某worker上的读取文件
     *
     * @param workerId    workerId
     * @param path        文件路径
     * @param fromLineNum 开始行数
     * @return 文件内容
     */
    TaskExecFileContent readFile(String workerId, String path, int fromLineNum);
}
