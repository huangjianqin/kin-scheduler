package org.kin.scheduler.core.log;

import ch.qos.logback.classic.Logger;
import org.kin.scheduler.core.utils.LogUtils;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class StaticLogger {
    public static Logger log;

    public static void init(String logPath) {
        if (Objects.isNull(log)) {
            synchronized (StaticLogger.class) {
                if (Objects.isNull(log)) {
                    log = LogUtils.getMasterLogger(logPath, "master");
                }
            }
        }
    }

    public static void init(String logPath, String workerId) {
        if (Objects.isNull(log)) {
            synchronized (StaticLogger.class) {
                if (Objects.isNull(log)) {
                    log = LogUtils.getWorkerLogger(logPath, workerId);
                }
            }
        }
    }
}
