package org.kin.scheduler.core.master.executor.allocate;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public enum AllocateStrategyType {
    /**
     * 根据资源分配
     */
    Res,
    /**
     * Hash分配
     */
    Hash,
    /**
     * 最不经常使用分配
     */
    LFU,
    /**
     * 最近最少使用分配
     */
    LRU,
    /**
     * 随机分配
     */
    Random,
    /**
     * 轮询分配
     */
    RoundRobin,
    /**
     * 获取全部executor
     */
    All,
    ;

    //------------------------------------------------------
    public static AllocateStrategyType[] VALUES = values();

    public static AllocateStrategyType getByName(String name) {
        for (AllocateStrategyType strategy : VALUES) {
            if (strategy.name().toLowerCase().equals(name.toLowerCase())) {
                return strategy;
            }
        }

        return null;
    }
}
