package org.kin.scheduler.core.task;

import java.util.Objects;

/**
 * task的执行策略
 *
 * @author huangjianqin
 * @date 2020-02-06
 */
public enum TaskExecStrategy {
    //串行
    SERIAL_EXECUTION("Serial"),
    //抛弃最新的, 以原来的task执行
    DISCARD_LATER("Discard Later"),
    //覆盖先前的, 以最新的task执行
    COVER_EARLY("Cover Early"),
    ;
    public static TaskExecStrategy[] VALUES = values();
    private String desc;

    TaskExecStrategy(String desc) {
        this.desc = desc;
    }

    /**
     * 根据类型名称获取类型实例
     */
    public static TaskExecStrategy getByName(String strategy) {
        for (TaskExecStrategy execStrategy : VALUES) {
            if (execStrategy.name().equals(strategy)) {
                return execStrategy;
            }
        }

        return null;
    }

    /**
     * 根据类型名称获取类型描述
     */
    public static String getDescByName(String strategy) {
        TaskExecStrategy taskExecStrategy = getByName(strategy);
        return Objects.nonNull(taskExecStrategy) ? taskExecStrategy.desc : "unknown: ".concat(strategy);
    }

    public String getDesc() {
        return desc;
    }
}
