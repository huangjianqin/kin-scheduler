package org.kin.scheduler.core.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

/**
 * task log上下文用于创建及管理Task相关logger
 *
 * @author huangjianqin
 * @date 2020-02-18
 */
public class TaskLoggerContext extends LoggerContext {
    public TaskLoggerContext(String executorId) {
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
