package org.kin.scheduler.core.log;

import ch.qos.logback.classic.Logger;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class Loggers {
    /** 理论上每个进程只有一个master or worker */
    private static Logger log;

    /**
     * task相关
     * task执行线程绑定的logger
     */
    private static ThreadLocal<Logger> threadLocalLogger = new ThreadLocal<>();
    /**
     * task output 路径
     */
    private static ThreadLocal<String> threadLocalTaskOutputFile = new ThreadLocal<>();

    //-----------------------------------------------------------------------------------------------------------------
    public static Logger master(String logPath, String masterId) {
        if (Objects.isNull(log)) {
            synchronized (Loggers.class) {
                if (Objects.isNull(log)) {
                    return log = LogUtils.getMasterLogger(logPath, masterId);
                }
            }
        }

        return null;
    }

    public static Logger worker(String logPath, String workerId) {
        if (Objects.isNull(log)) {
            synchronized (Loggers.class) {
                if (Objects.isNull(log)) {
                    return log = LogUtils.getWorkerLogger(logPath, workerId);
                }
            }
        }

        return null;
    }

    //-----------------------------------------------------------------------------------------------------------------
    public static void updateLogger(Logger log) {
        threadLocalLogger.set(log);
    }

    public static Logger logger() {
        return threadLocalLogger.get();
    }

    public static void updateTaskOutputFileName(String logFile) {
        threadLocalTaskOutputFile.set(logFile);
    }

    public static String getTaskOutputFileName() {
        return threadLocalTaskOutputFile.get();
    }

    public static void removeAll() {
        Logger logger = threadLocalLogger.get();
        if (Objects.nonNull(logger)) {
            logger.detachAndStopAllAppenders();
        }
        threadLocalLogger.remove();
        threadLocalTaskOutputFile.remove();
    }
}
