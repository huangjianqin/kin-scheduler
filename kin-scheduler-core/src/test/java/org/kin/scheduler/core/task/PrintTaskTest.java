package org.kin.scheduler.core.task;

import org.kin.scheduler.core.driver.impl.JobContext;

/**
 * @author huangjianqin
 * @date 2020-02-15
 */
public class PrintTaskTest {
    public static void main(String[] args) {
        JobContext context = JobContext.build().appName("print").master("0.0.0.0:46668");
        try{
            context.run("test");
        }finally {
            context.stop();
        }

    }
}
