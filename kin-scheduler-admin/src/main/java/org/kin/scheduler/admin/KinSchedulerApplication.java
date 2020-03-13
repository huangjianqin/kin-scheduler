package org.kin.scheduler.admin;

import org.kin.scheduler.admin.core.TaskScheduleKeeper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@SpringBootApplication
@EnableCaching
@EnableTransactionManagement(proxyTargetClass = true)
public class KinSchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(KinSchedulerApplication.class);
        try {
            TaskScheduleKeeper.instance();
            synchronized (KinSchedulerApplication.class) {
                KinSchedulerApplication.class.wait();
            }
        } catch (InterruptedException e) {

        } finally {
            TaskScheduleKeeper.instance().stop();
        }
    }
}
