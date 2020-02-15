package org.kin.scheduler.core.task.handler;

import org.kin.scheduler.core.task.Task;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * 必须包含无参constructor
 * 单例模式调用
 */
public interface TaskHandler<PARAM extends Serializable> {
    /**
     * @return 返回该handler处理的Task 参数类型
     */
    Class<PARAM> getTaskParamType();

    /**
     * 处理Task
     *
     * @param Task task信息
     */
    Object exec(Task<PARAM> Task);
}
