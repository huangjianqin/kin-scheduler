package org.kin.scheduler.core.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.kin.framework.log.logback.LogbackFactory;

import java.io.File;

/**
 * @author huangjianqin
 * @date 2020-02-19
 */
public class LogUtils {
    public static final String BASE_PATH = "/logs";

    public static Logger getMasterLogger(String basePath, String masterId) {
        String loggerName = masterId.concat("Logger");
        String appenderName = masterId.concat("Appender");
        return getLogger(basePath, loggerName, appenderName, masterId);
    }

    public static Logger getWorkerLogger(String basePath, String workerId) {
        String loggerName = workerId.concat("Logger");
        String appenderName = workerId.concat("Appender");
        return getLogger(basePath, loggerName, appenderName, workerId);
    }

    public static Logger getExecutorLogger(String basePath, String workerId, String executorId) {
        String loggerName = executorId.concat("Logger");
        String appenderName = executorId.concat("Appender");
        return getLogger(basePath, loggerName, appenderName, workerId.concat(File.separator).concat(executorId));
    }

    public static Logger getLogger(String basePath, String loggerName, String appenderName, String file) {
        LoggerContext lc = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        return getLogger(lc, basePath, loggerName, appenderName, file);
    }

    public static Logger getLogger(LoggerContext lc, String basePath, String loggerName, String appenderName, String file) {
        ThresholdFilter infoFilter = new ThresholdFilter();
        infoFilter.setLevel("INFO");
        infoFilter.setContext(lc);
        infoFilter.start();

        TimeBasedRollingPolicy policy = new TimeBasedRollingPolicy();
        policy.setFileNamePattern(basePath.concat(File.separator).concat("%d{yyyy-MM-dd}").concat(File.separator).concat(file).concat(".log"));
//        policy.setMaxHistory(30);
        policy.setContext(lc);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setPattern("[%p] %d{yyyy-MM-dd HH:mm:ss SSS} [%t] |  %C.%M\\(%L\\) : %msg%n%ex");
        encoder.start();

        RollingFileAppender<ILoggingEvent> dailyRollingFileAppender = new RollingFileAppender<>();
        dailyRollingFileAppender.setContext(lc);
        dailyRollingFileAppender.setName(appenderName);
        dailyRollingFileAppender.addFilter(infoFilter);
        dailyRollingFileAppender.setRollingPolicy(policy);
        dailyRollingFileAppender.setEncoder(encoder);
        dailyRollingFileAppender.setAppend(true);

        //下面三行很关键
        policy.setParent(dailyRollingFileAppender);
        policy.start();
        dailyRollingFileAppender.start();

        return LogbackFactory.create(loggerName, lc).add(dailyRollingFileAppender).level(Level.INFO).additive(false).get();
    }

}
