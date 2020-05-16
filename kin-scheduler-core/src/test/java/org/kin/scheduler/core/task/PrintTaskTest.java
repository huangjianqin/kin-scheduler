package org.kin.scheduler.core.task;

import org.kin.scheduler.core.driver.impl.SimpleDriver;
import org.kin.scheduler.core.driver.schedule.impl.SchedulerContextImpl;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategyType;

/**
 * @author huangjianqin
 * @date 2020-02-15
 */
public class PrintTaskTest {
    public static void main(String[] args) {
        SimpleDriver driver = new SimpleDriver(SchedulerContextImpl.build().appName("test").allocateStrategy(AllocateStrategyType.All));
        driver.init();
        driver.start();
    }
}
