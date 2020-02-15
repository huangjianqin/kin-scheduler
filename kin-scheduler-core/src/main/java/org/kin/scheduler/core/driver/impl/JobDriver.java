package org.kin.scheduler.core.driver.impl;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.scheduler.core.driver.Driver;
import org.kin.scheduler.core.driver.TaskSubmitFuture;
import org.kin.scheduler.core.task.Task;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class JobDriver extends Driver {
    private JobTaskScheduler taskScheduler;
    private AtomicInteger taskIdCounter = new AtomicInteger();

    public JobDriver(JobContext jobContext) {
        super(jobContext);
    }

    @Override
    public void start() {
        super.start();
        taskScheduler = new JobTaskScheduler(job);
        taskScheduler.init();
        taskScheduler.start();
    }

    @Override
    public void close() {
        super.close();
        if(Objects.nonNull(taskScheduler)){
            taskScheduler.close();
        }
    }

    private void assignTaskId(Task task) {
        task.setJobId(job.getJobId());
        task.setTaskId(task.getJobId().concat("-task").concat(String.valueOf(taskIdCounter.getAndIncrement())));
    }

    public <R> TaskSubmitFuture<R> submitTask(Task task) {
        if (isInState(State.STARTED)) {
            assignTaskId(task);
            return taskScheduler.submitTask(task);
        }

        //TODO
        return null;
    }

    public <R> R submitTaskSync(Task task) {
        try {
            return (R) submitTask(task).get();
        } catch (InterruptedException e) {

        } catch (ExecutionException e) {
            ExceptionUtils.log(e);
        }

        return null;
    }
}
