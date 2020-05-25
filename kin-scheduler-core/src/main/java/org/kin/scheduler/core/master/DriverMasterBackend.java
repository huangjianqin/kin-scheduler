package org.kin.scheduler.core.master;

import org.kin.scheduler.core.driver.transport.ApplicationRegisterInfo;
import org.kin.scheduler.core.master.transport.ApplicationRegisterResponse;

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

}
