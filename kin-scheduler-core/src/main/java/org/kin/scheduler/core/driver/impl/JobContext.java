package org.kin.scheduler.core.driver.impl;

import org.kin.framework.JvmCloseCleaner;
import org.kin.scheduler.core.driver.TaskSubmitFuture;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.task.TaskExecStrategy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-11
 */
public class JobContext {
    private static Map<String, JobDriver> DRIVERS = new HashMap<>();

    static {
        JvmCloseCleaner.DEFAULT().add(JvmCloseCleaner.MAX_PRIORITY, () -> {
            for (JobDriver driver : DRIVERS.values()) {
                driver.close();
            }
        });
    }

    private String appName;
    /** master rpc接口 */
    private String masterAddress;
    /** 并发 */
    private int parallelism = 1;

    public JobContext() {
    }

    public JobContext(String appName) {
        this.appName = appName;
    }

    //-----------------------------------------------------------------------------------------------

    public static JobContext build() {
        return new JobContext();
    }

    public static JobContext build(String appName) {
        return new JobContext(appName);
    }

    //-----------------------------------------------------------------------------------------------

    public JobContext appName(String appName) {
        this.appName = appName;
        return this;
    }

    public JobContext master(String masterAddress) {
        this.masterAddress = masterAddress;
        return this;
    }

    public JobContext parallelism(int parallelism) {
        this.parallelism = parallelism;
        return this;
    }

    private JobDriver getOrCreateDriver() {
        JobDriver driver = DRIVERS.get(appName);
        if (Objects.isNull(driver)) {
            synchronized (JobContext.class) {
                driver = DRIVERS.get(appName);
                if (Objects.isNull(driver)) {
                    driver = new JobDriver(this);
                    driver.init();
                    driver.start();
                    DRIVERS.put(appName, driver);
                }
            }
        }

        return driver;
    }

    public <PARAM extends Serializable, R> TaskSubmitFuture<R> run(PARAM param, TaskExecStrategy execStrategy, int timeout) {
        JobDriver driver = getOrCreateDriver();
        return driver.submitTask(Task.createTmpTask(param, execStrategy, timeout));
    }

    public <PARAM extends Serializable, R> TaskSubmitFuture<R> run(PARAM param, TaskExecStrategy execStrategy) {
        return run(param, execStrategy, -1);
    }

    public <PARAM extends Serializable, R> TaskSubmitFuture<R> run(PARAM param, int timeout) {
        return run(param, TaskExecStrategy.SERIAL_EXECUTION, timeout);
    }

    public <PARAM extends Serializable, R> TaskSubmitFuture<R> run(PARAM param) {
        return run(param, TaskExecStrategy.SERIAL_EXECUTION, -1);
    }

    public <PARAM extends Serializable, R> R runSync(PARAM param, TaskExecStrategy execStrategy, int timeout) {
        JobDriver driver = getOrCreateDriver();
        return driver.submitTaskSync(Task.createTmpTask(param, execStrategy, timeout));
    }

    public <PARAM extends Serializable, R> R runSync(PARAM param, TaskExecStrategy execStrategy) {
        return runSync(param, execStrategy, -1);
    }

    public <PARAM extends Serializable, R> R runSync(PARAM param, int timeout) {
        return runSync(param, TaskExecStrategy.SERIAL_EXECUTION, timeout);
    }

    public <PARAM extends Serializable, R> R runSync(PARAM param) {
        return runSync(param, TaskExecStrategy.SERIAL_EXECUTION, -1);
    }

    public void stop() {
        for (JobDriver driver : DRIVERS.values()) {
            driver.close();
        }
        DRIVERS.clear();
        System.exit(0);
    }

    //getter

    public String getAppName() {
        return appName;
    }

    public String getMasterAddress() {
        return masterAddress;
    }

    public int getParallelism() {
        return parallelism;
    }
}
