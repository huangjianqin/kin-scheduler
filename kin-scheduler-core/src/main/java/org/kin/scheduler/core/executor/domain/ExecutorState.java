package org.kin.scheduler.core.executor.domain;

/**
 * executor运行状态
 *
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

    /**
     * @return 是否结束, 失败/被杀死进程/正常退出都算
     */
    public boolean isFinished() {
        return equals(ExecutorState.FAIL) || equals(ExecutorState.KILLED) || equals(ExecutorState.EXIT);
    }
}
