package org.kin.scheduler.core.master.executor.allocate;

/**
 * application资源分配策略类型
 *
 * @author huangjianqin
 * @date 2020-03-03
 */
public enum AllocateStrategyType {
    /**
     * 根据资源分配
     */
    RES,
    /**
     * Hash分配
     */
    HASH,
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
    RANDOM,
    /**
     * 轮询分配
     */
    ROUND_ROBIN,
    /**
     * 获取全部executor
     */
    ALL,
    ;

    //------------------------------------------------------
    public static AllocateStrategyType[] VALUES = values();

    /**
     * 根据类型名字获取 资源分配策略类型
     */
    public static AllocateStrategyType getByName(String name) {
        for (AllocateStrategyType strategy : VALUES) {
            if (strategy.name().toLowerCase().equals(name.toLowerCase())) {
                return strategy;
            }
        }

        return null;
    }
}
