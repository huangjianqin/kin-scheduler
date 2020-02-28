package org.kin.scheduler.core.task;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * task的执行策略
 */
public enum TaskExecStrategy {
    //串行
    SERIAL_EXECUTION("Serial"),
    //抛弃最新的, 以原来的task执行
    DISCARD_LATER("Discard Later"),
    //覆盖先前的, 以最新的task执行
    COVER_EARLY("Cover Early"),
    ;

    private String desc;

    TaskExecStrategy(String desc) {
        this.desc = desc;
    }

    //getter

    public String getDesc() {
        return desc;
    }
}
