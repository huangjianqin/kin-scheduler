package org.kin.scheduler.core.log;

/**
 * @author huangjianqin
 * @date 2020-05-29
 */
public class StaticLoggerTest {
    public static void main(String[] args) {
        Loggers.master("logs", "master");
        Loggers.log.info("测试");
    }
}
