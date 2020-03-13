package org.kin.scheduler.core.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.kin.scheduler.core.utils.LogUtils;

import java.text.SimpleDateFormat;

/**
 * @author huangjianqin
 * @date 2020-02-18
 * <p>
 * 日志目录
 * basePath/{yyyy-MM-dd}/{workerId}.log
 * basePath/{yyyy-MM-dd}/{workerId}/{executorId}.log
 * basePath/{yyyy-MM-dd}/{masterId}.log
 * basePath/{yyyy-MM-dd}/jobs/{jobId}/{taskId}/{logFileName}.log
 */
public class LogContext extends LoggerContext {
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public LogContext(String executorId) {
        setName(executorId);
    }

    public Logger getTaskLogger(String basePath, String jobId, String taskId, String logFileName) {
        String loggerName = jobId.concat("Logger");
        String appenderName = jobId.concat("Appender");
        return LogUtils.getLogger(this, loggerName, appenderName, LogUtils.getTaskLogFileName(basePath, jobId, taskId, logFileName));
    }

    @Override
    public void stop() {
        for (ch.qos.logback.classic.Logger logger : getLoggerList()) {
            logger.detachAndStopAllAppenders();
        }
        super.stop();
    }
}
