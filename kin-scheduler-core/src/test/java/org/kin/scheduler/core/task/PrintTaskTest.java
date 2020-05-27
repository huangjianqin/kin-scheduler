package org.kin.scheduler.core.task;

import org.kin.scheduler.core.driver.impl.SimpleApplication;
import org.kin.scheduler.core.driver.impl.SimpleDriver;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategyType;

/**
 * @author huangjianqin
 * @date 2020-02-15
 */
public class PrintTaskTest {
    public static void main(String[] args) throws InterruptedException {
        SimpleDriver driver = new SimpleDriver(SimpleApplication.build().appName("test").allocateStrategy(AllocateStrategyType.All));
        driver.init();
        driver.start();

        Thread.sleep(3000);

        TaskDescription<String> printTaskDescription = new TaskDescription<>("job1", "test1");
        printTaskDescription.setParam("haha");

        driver.submitTask(printTaskDescription);

    }
}
