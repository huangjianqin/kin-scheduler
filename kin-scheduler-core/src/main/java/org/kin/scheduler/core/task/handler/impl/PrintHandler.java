package org.kin.scheduler.core.task.handler.impl;

import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.task.handler.TaskHandler;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * 打印Task信息的handler
 */
public class PrintHandler implements TaskHandler<String> {
    @Override
    public Class<String> getTaskParamType() {
        return String.class;
    }

    @Override
    public Serializable exec(Task<String> Task) {
        System.out.println(Task.getJobId());
        System.out.println(Task.getTaskId());
        System.out.println(Task.getExecStrategy());
        System.out.println(Task.getParam());

        return null;
    }
}
