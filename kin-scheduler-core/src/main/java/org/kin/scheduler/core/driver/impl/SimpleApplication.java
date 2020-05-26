package org.kin.scheduler.core.driver.impl;

import org.kin.scheduler.core.driver.Application;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public class SimpleApplication extends Application {
    public SimpleApplication() {
    }

    public SimpleApplication(String appName) {
        super(appName);
    }

    public static Application build() {
        return new SimpleApplication();
    }

    public static Application build(String appName) {
        return new SimpleApplication(appName);
    }
}
