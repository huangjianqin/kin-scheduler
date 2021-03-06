package org.kin.scheduler.core.task.handler;

import org.kin.scheduler.core.log.Loggers;
import org.kin.scheduler.core.task.TaskDescription;

import java.io.Serializable;

/**
 * 必须包含无参constructor
 * 单例模式调用
 * {@link Loggers} 获取当前日志输出
 *
 * @author huangjianqin
 * @date 2020-02-06
 */
public interface TaskHandler<PARAM extends Serializable, RESULT extends Serializable> {
    /**
     * 返回该handler处理的Task 参数类型
     *
     * @return handler处理的Task 参数类型
     */
    Class<PARAM> getTaskParamType();

    /**
     * 处理Task
     *
     * @param taskDescription task信息
     * @return task执行结果
     * @throws Exception 异常
     */
    RESULT exec(TaskDescription<PARAM> taskDescription) throws Exception;
}
