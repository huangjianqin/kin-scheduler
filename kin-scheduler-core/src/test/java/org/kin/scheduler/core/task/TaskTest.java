package org.kin.scheduler.core.task;

import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.driver.Driver;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategyType;
import org.kin.scheduler.core.worker.transport.TaskExecFileContent;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-15
 */
public abstract class TaskTest {
    protected void run() throws InterruptedException {
        Driver driver = Driver.common(Application.build().appName("test").allocateStrategy(AllocateStrategyType.All));
        driver.start();

        Thread.sleep(500);

        TaskDescription<?> taskDescription = generateTaskDescription();
        taskDescription.setJobId("job-test");

        TaskExecFuture<Serializable> taskExecFuture = driver.submitTask(taskDescription);
        driver.awaitTermination();
        String taskId = taskExecFuture.getTaskSubmitResp().getTaskId();
        TaskExecFileContent log = driver.readLog(taskId, 0);
        TaskExecFileContent output = driver.readOutput(taskId, 0);
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("任务id:" + taskId);
        System.out.println(log.getPath());
        System.out.println(log.getContent());
        System.out.println("-------------------------");
        System.out.println(output.getPath());
        System.out.println(output.getContent());
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------");
        driver.stop();
    }

    abstract TaskDescription<?> generateTaskDescription();
}
