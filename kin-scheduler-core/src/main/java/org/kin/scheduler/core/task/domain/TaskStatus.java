package org.kin.scheduler.core.task.domain;

/**
 * @author huangjianqin
 * @date 2020-05-27
 */
public enum TaskStatus {
    /** 启动中 */
    LAUNCHING,
    /** 运行中 */
    RUNNING,
    /** 失败 */
    FAIL,
    /** 取消 */
    CANCELLED,
    /** 完成 */
    FINISHED,
    /** 丢失 */
    LOST,
    ;

    public boolean isFinished() {
        return equals(TaskStatus.FAIL) ||
                equals(TaskStatus.CANCELLED) ||
                equals(TaskStatus.FINISHED) ||
                equals(TaskStatus.LOST);
    }

}
