package org.kin.scheduler.core.executor;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.kin.scheduler.core.utils.LogUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author huangjianqin
 * @date 2020-02-18
 * <p>
 * TODO 暂不细分日志(每个task一个日志)
 * 日志目录
 * basePath/{yyyy-MM-dd}/{workerId}.log
 * basePath/{yyyy-MM-dd}/{masterId}.log
 * basePath/{yyyy-MM-dd}/jobs/{jobId}.log
 */
public class LogContext extends LoggerContext {
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public LogContext(String executorId) {
        setName(executorId);
    }

    private String getFile(String jobId) {
        return "jobs".concat(File.separator).concat(jobId);
    }

    public Logger getJobLogger(String basePath, String jobId) {
        String loggerName = jobId.concat("Logger");
        String appenderName = jobId.concat("Appender");
        String file = getFile(jobId);
        return LogUtils.getLogger(this, basePath, loggerName, appenderName, file);
    }

    public String getJobLogFile(String basePath, String jobId) {
        String file = getFile(jobId);
        return basePath.concat(File.separator).concat(df.format(new Date())).concat(File.separator).concat(file).concat(".log");
    }

    @Override
    public void stop() {
        for (ch.qos.logback.classic.Logger logger : getLoggerList()) {
            logger.detachAndStopAllAppenders();
        }
        super.stop();
    }
}
