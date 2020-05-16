package org.kin.scheduler.core.driver.schedule.impl;

import org.kin.scheduler.core.driver.SchedulerContext;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public class SchedulerContextImpl extends SchedulerContext {
    public SchedulerContextImpl() {
    }

    public SchedulerContextImpl(String appName) {
        super(appName);
    }

    public static SchedulerContext build() {
        return new SchedulerContextImpl();
    }

    public static SchedulerContext build(String appName) {
        return new SchedulerContextImpl(appName);
    }
}
