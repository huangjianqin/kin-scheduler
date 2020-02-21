package org.kin.scheduler.core.task.handler.impl;

import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.domain.Singleton;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * 打印Task信息的handler
 */
@Singleton
public class PrintHandler implements TaskHandler<String, Integer> {
    @Override
    public Class<String> getTaskParamType() {
        return String.class;
    }

    @Override
    public Integer exec(Task<String> task) throws Exception {
        System.out.println(task.getJobId());
        System.out.println(task.getTaskId());
        System.out.println(task.getExecStrategy());
        System.out.println(task.getParam());

        return 1;
    }
}
