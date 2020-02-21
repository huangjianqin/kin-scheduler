package org.kin.scheduler.core.task;

import ch.qos.logback.classic.Logger;

/**
 * @author huangjianqin
 * @date 2020-02-20
 */
public class TaskLoggers {
    private static ThreadLocal<Logger> threadLocalLogger = new ThreadLocal<>();
    private static ThreadLocal<String> threadLocalLoggerFile = new ThreadLocal<>();

    public static void updateLogger(Logger log){
        threadLocalLogger.set(log);
    }

    public static Logger getLogger(){
        return threadLocalLogger.get();
    }

    public static void updateLoggerFile(String logFile){
        threadLocalLoggerFile.set(logFile);
    }

    public static String getLoggerFile(){
        return threadLocalLoggerFile.get();
    }
}
