package org.kin.scheduler.core.task.handler.impl;

import org.kin.scheduler.core.task.TaskDescription;
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
    public Integer exec(TaskDescription<String> taskDescription) throws Exception {
        System.out.println(taskDescription.getJobId());
        System.out.println(taskDescription.getTaskId());
        System.out.println(taskDescription.getExecStrategy());
        System.out.println(taskDescription.getParam());

        return 1;
    }
}
