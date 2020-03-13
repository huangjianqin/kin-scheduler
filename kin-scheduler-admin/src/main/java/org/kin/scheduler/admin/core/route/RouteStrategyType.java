package org.kin.scheduler.admin.core.route;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public enum RouteStrategyType {
    /**
     * Hash分配
     */
    Hash("Hash分配"),
    /**
     * 最不经常使用分配
     */
    LFU("最不经常使用分配"),
    /**
     * 最近最少使用分配
     */
    LRU("最近最少使用分配"),
    /**
     * 随机分配
     */
    Random("随机分配"),
    /**
     * 轮询分配
     */
    RoundRobin("轮询分配"),
    ;

    //------------------------------------------------------
    public static RouteStrategyType[] VALUES = values();

    private String desc;

    RouteStrategyType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public static RouteStrategyType getByName(String name) {
        for (RouteStrategyType strategy : VALUES) {
            if (strategy.name().toLowerCase().equals(name.toLowerCase())) {
                return strategy;
            }
        }

        return null;
    }
}
