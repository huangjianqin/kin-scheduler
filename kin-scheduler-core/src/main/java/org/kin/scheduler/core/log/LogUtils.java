package org.kin.scheduler.core.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import org.kin.framework.log.logback.LogbackFactory;
import org.kin.framework.utils.StringUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-19
 */
public class LogUtils {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static Logger getMasterLogger(String basePath, String masterId) {
        String loggerName = masterId.concat("Logger");
        String appenderName = masterId.concat("Appender");
        return getLogger(loggerName, appenderName, getMasterLogFileName(basePath, masterId));
    }

    public static String getMasterLogFileName(String basePath, String masterId) {
        return getLogFileName(basePath, masterId);
    }

    public static Logger getWorkerLogger(String basePath, String workerId) {
        String loggerName = workerId.concat("Logger");
        String appenderName = workerId.concat("Appender");
        return getLogger(loggerName, appenderName, getWorkerLogFileName(basePath, workerId));
    }

    public static String getWorkerLogFileName(String basePath, String workerId) {
        return getLogFileName(basePath, workerId);
    }

    public static Logger getExecutorLogger(String basePath, String workerId, String executorId) {
        String loggerName = executorId.concat("Logger");
        String appenderName = executorId.concat("Appender");
        return getLogger(loggerName, appenderName, getExecutorLogFileName(basePath, workerId, executorId));
    }

    public static String getExecutorLogFileName(String basePath, String workerId, String executorId) {
        return getLogFileName(basePath, workerId.concat(File.separator).concat(executorId));
    }

    /**
     * @return task log name
     */
    public static String getTaskLogFileName(String basePath, String jobId, String taskId, String logFileName) {
        String fileName = "jobs".concat(File.separator).concat(jobId).concat(File.separator).concat(taskId);
        if (StringUtils.isNotBlank(logFileName)) {
            fileName = fileName.concat(File.separator).concat(logFileName);
        }
        return getLogFileName(basePath, fileName);
    }

    public static String getTaskLogFileAbsoluteName(String basePath, String jobId, String taskId, String logFileName) {
        File logFile = new File(getTaskLogFileName(basePath, jobId, taskId, logFileName));
        return logFile.getAbsolutePath();
    }

    /**
     * @return task output name
     */
    public static String getTaskOutputFileName(String basePath, String jobId, String taskId, String logFileName) {
        String fileName = "jobs".concat(File.separator).concat(jobId).concat(File.separator).concat(taskId);
        if (StringUtils.isNotBlank(logFileName)) {
            fileName = fileName.concat(File.separator).concat(logFileName);
        }
        return getFileName(basePath, fileName, ".out");
    }

    public static String getTaskOutputFileAbsoluteName(String basePath, String jobId, String taskId, String logFileName) {
        File logFile = new File(getTaskOutputFileName(basePath, jobId, taskId, logFileName));
        return logFile.getAbsolutePath();
    }

    /**
     * 获取log文件路径
     *
     * @param basePath log目录
     * @param file     自定义log文件路径
     * @return log文件路径
     */
    public static String getLogFileName(String basePath, String file) {
        return getFileName(basePath, file, ".log");
    }

    /**
     * 获取文件路径
     *
     * @param basePath 基础目录
     * @param file     自定义文件路径
     * @param suffix   后缀
     * @return 文件路径
     */
    public static String getFileName(String basePath, String file, String suffix) {
        return basePath.concat(File.separator).concat(DATE_FORMAT.format(new Date())).concat(File.separator).concat(file).concat(suffix);
    }

    public static Logger getLogger(String loggerName, String appenderName, String file) {
        LoggerContext lc = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        return getLogger(lc, loggerName, appenderName, file);
    }

    public static Logger getLogger(LoggerContext lc, String loggerName, String appenderName, String file) {
        ThresholdFilter infoFilter = new ThresholdFilter();
        infoFilter.setLevel("INFO");
        infoFilter.setContext(lc);
        infoFilter.start();

//        TimeBasedRollingPolicy policy = new TimeBasedRollingPolicy();
//        policy.setFileNamePattern(basePath.concat(File.separator).concat("%d{yyyy-MM-dd}").concat(File.separator).concat(file).concat(".log"));
////        policy.setMaxHistory(30);
//        policy.setContext(lc);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setPattern("[%p] %d{yyyy-MM-dd HH:mm:ss SSS} [%t] |  %C.%M\\(%L\\) : %msg%n%ex");
        encoder.start();

//        RollingFileAppender<ILoggingEvent> dailyRollingFileAppender = new RollingFileAppender<>();
//        dailyRollingFileAppender.setContext(lc);
//        dailyRollingFileAppender.setName(appenderName);
//        dailyRollingFileAppender.addFilter(infoFilter);
//        dailyRollingFileAppender.setRollingPolicy(policy);
//        dailyRollingFileAppender.setEncoder(encoder);
//        dailyRollingFileAppender.setAppend(true);

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setContext(lc);
        fileAppender.setName(appenderName);
        fileAppender.addFilter(infoFilter);
        fileAppender.setEncoder(encoder);
        fileAppender.setAppend(true);

        fileAppender.setFile(file);

//        policy.setParent(dailyRollingFileAppender);
//        policy.start();
//        dailyRollingFileAppender.start();

        fileAppender.start();

        //从已有的logger中获取打印控制台的appender
        Appender<ILoggingEvent> consoleAppender = null;
        for (Logger logger : lc.getLoggerList()) {
            consoleAppender = logger.getAppender("CONSOLE");
            if (Objects.nonNull(consoleAppender)) {
                break;
            }
        }

        LogbackFactory logbackFactory = LogbackFactory.create(loggerName, lc);
        logbackFactory.add(fileAppender);
        if (Objects.nonNull(consoleAppender)) {
            logbackFactory.add(consoleAppender);
        }
        return logbackFactory.level(Level.INFO).additive(false).get();
    }

}