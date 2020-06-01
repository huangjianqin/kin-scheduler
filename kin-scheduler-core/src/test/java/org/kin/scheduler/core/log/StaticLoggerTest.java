package org.kin.scheduler.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2020-05-29
 */
public class StaticLoggerTest {
    private static final Logger log = LoggerFactory.getLogger(StaticLoggerTest.class);

    public static void main(String[] args) {
        log.info("start");
        Loggers.master("logs", "master");
        Loggers.log.info("测试");
    }
}
