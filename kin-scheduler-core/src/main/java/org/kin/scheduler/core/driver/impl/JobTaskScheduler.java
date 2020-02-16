package org.kin.scheduler.core.driver.impl;

import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.scheduler.core.driver.Job;
import org.kin.scheduler.core.driver.TaskContext;
import org.kin.scheduler.core.driver.TaskScheduler;
import org.kin.scheduler.core.driver.TaskSubmitFuture;
import org.kin.scheduler.core.driver.exception.TaskRetryTimesOutException;
import org.kin.scheduler.core.executor.ExecutorBackend;
import org.kin.scheduler.core.executor.domain.TaskExecResult;
import org.kin.scheduler.core.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-12
 */
public class JobTaskScheduler extends TaskScheduler {
    private static final Logger log = LoggerFactory.getLogger(JobTaskScheduler.class);

    private ThreadManager threads;

    public JobTaskScheduler(Job job) {
        super(job);
    }

    @Override
    public void init() {
        super.init();
        threads = new ThreadManager(
                new ThreadPoolExecutor(SysUtils.getSuitableThreadNum(), SysUtils.getSuitableThreadNum(), 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
                SysUtils.CPU_NUM);
    }

    public <R> TaskSubmitFuture<R> submitTask(Task task) {
        if (isInState(State.STARTED)) {
            TaskContext taskContext = taskSetManager.init(Collections.singletonList(task)).get(0);
            //调度task
            TaskExecRunnable execRunnable = new TaskExecRunnable(taskContext);
            TaskSubmitFuture future = new TaskSubmitFuture(taskSetManager, taskContext);
            taskContext.submitTask(future, execRunnable);
            threads.execute(execRunnable);
            log.debug("submitTask >>>> {}", taskContext.getTask());
            return future;
        }

        return null;
    }

    @Override
    public void close() {
        if (Objects.nonNull(threads)) {
            threads.shutdownNow();
        }
        super.close();
        log.info("JobTaskScheduler closed");
    }

    public class TaskExecRunnable implements Runnable {
        private TaskContext taskContext;
        private Random random = new Random();
        private Thread thread;

        public TaskExecRunnable(TaskContext taskContext) {
            this.taskContext = taskContext;
        }

        @Override
        public void run() {
            thread = Thread.currentThread();
            try {
                List<Map.Entry<String, ExecutorBackend>> executorBackends = JobTaskScheduler.this.executorBackends.entrySet().stream()
                        .filter(entry -> !taskContext.getExecedExecutorIds().contains(entry.getKey()))
                        .collect(Collectors.toList());

                if (CollectionUtils.isNonEmpty(executorBackends)) {
                    Map.Entry<String, ExecutorBackend> entry = executorBackends.get(random.nextInt(executorBackends.size()));

                    Task task = taskContext.getTask();
                    taskContext.exec(entry.getKey());
                    TaskExecResult execResult = entry.getValue().execTask(task);
                    if (Objects.nonNull(execResult)) {
                        if (execResult.isSuccess()) {
                            taskSetManager.taskFinish(task.getTaskId(), execResult.getExecResult());
                            log.info("Task({}) finished, result >>>> {}", task.getTaskId(), execResult.getExecResult());
                        }
                        if (execResult.isNeedRetry() && taskContext.retry()) {
                            run();
                        }
                    }
                }
            } catch (Exception e) {
                if (!TaskRetryTimesOutException.class.isAssignableFrom(e.getClass()) &&
                        !InterruptedException.class.isAssignableFrom(e.getClass())) {
                    if (taskContext.retry()) {
                        run();
                    }
                }
            }
        }

        public void interrupt() {
            thread.interrupt();
        }
    }
}
