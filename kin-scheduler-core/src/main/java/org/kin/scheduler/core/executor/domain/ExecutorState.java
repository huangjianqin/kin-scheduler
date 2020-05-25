package org.kin.scheduler.core.executor.domain;

/**
 * @author huangjianqin
 * @date 2020-05-25
 */
public enum ExecutorState {
    /** 启动中 */
    LAUNCHING,
    /** 运行中 */
    RUNNING,
    /** 失败 */
    FAIL,
    /** 强制杀死进程 */
    KILLED,
    /** 正常退出 */
    EXIT;


}
