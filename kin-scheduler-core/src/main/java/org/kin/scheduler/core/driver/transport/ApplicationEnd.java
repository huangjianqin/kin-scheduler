package org.kin.scheduler.core.driver.transport;

import java.io.Serializable;

/**
 * application 结束消息
 *
 * @author huangjianqin
 * @date 2020-06-16
 */
public class ApplicationEnd implements Serializable {
    private static final long serialVersionUID = 8579629061761724034L;
    /** application名 */
    private String appName;

    public ApplicationEnd() {
    }

    public static ApplicationEnd of(String appName) {
        ApplicationEnd message = new ApplicationEnd();
        message.appName = appName;
        return message;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
