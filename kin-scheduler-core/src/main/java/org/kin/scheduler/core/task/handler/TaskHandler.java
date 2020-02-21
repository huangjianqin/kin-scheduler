package org.kin.scheduler.core.task.handler;

import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.task.TaskLoggers;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * 必须包含无参constructor
 * 单例模式调用
 * <p>
 * {@link TaskLoggers} 获取当前日志输出
 */
public interface TaskHandler<PARAM extends Serializable, RESULT extends Serializable> {
    /**
     * @return 返回该handler处理的Task 参数类型
     */
    Class<PARAM> getTaskParamType();

    /**
     * 处理Task
     *
     * @param Task task信息
     */
    RESULT exec(Task<PARAM> task) throws Exception;
}
