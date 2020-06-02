package org.kin.scheduler.core.task;

import org.kin.scheduler.core.driver.impl.SimpleApplication;
import org.kin.scheduler.core.driver.impl.SimpleDriver;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategyType;

/**
 * @author huangjianqin
 * @date 2020-02-15
 */
public abstract class TaskTest {
    protected void run() throws InterruptedException {
        SimpleDriver driver = new SimpleDriver(SimpleApplication.build().appName("test").allocateStrategy(AllocateStrategyType.All));
        driver.init();
        driver.start();

        Thread.sleep(1000);

        TaskDescription<?> taskDescription = generateTaskDescription();
        taskDescription.setJobId("job-test");
        taskDescription.setTaskId("task-test");

        driver.submitTask(taskDescription);
        driver.awaitTermination();
        driver.stop();
    }

    abstract TaskDescription<?> generateTaskDescription();
}
