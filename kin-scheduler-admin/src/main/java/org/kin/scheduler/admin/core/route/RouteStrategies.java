package org.kin.scheduler.admin.core.route;

import org.kin.framework.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RouteStrategies {
    private static Logger log = LoggerFactory.getLogger(RouteStrategies.class);
    private static Map<RouteStrategyType, RouteStrategy> strategies;

    static {
        Set<Class<? extends RouteStrategy>> allocateStrategyImplClasses =
                ClassUtils.getSubClass(RouteStrategy.class.getPackage().getName(), RouteStrategy.class, false);
        Map<RouteStrategyType, RouteStrategy> strategies = new EnumMap<>(RouteStrategyType.class);
        for (Class<? extends RouteStrategy> allocateStrategyImplClass : allocateStrategyImplClasses) {
            try {
                String className = allocateStrategyImplClass.getSimpleName();
                int prefix = className.lastIndexOf(RouteStrategy.class.getSimpleName());
                String allocateStrategyName = className.substring(0, prefix);
                RouteStrategyType routeStrategyType = RouteStrategyType.valueOf(allocateStrategyName);
                Constructor<? extends RouteStrategy> constructor = allocateStrategyImplClass.getConstructor();
                strategies.put(routeStrategyType, constructor.newInstance());
            } catch (Exception e) {
                log.error("init route strategy impl class({}) error >>> {}", allocateStrategyImplClass.getName(), e);
            }
        }

        RouteStrategies.strategies = strategies;
    }

    //----------------------------------------------------------------------------------

    /**
     * 根据类型获取executor分配策略实现类实例
     *
     * @param name executor分配策略类型名
     * @return executor分配策略实现类实例
     */
    public static RouteStrategy getByName(String name) {
        try {
            return strategies.get(RouteStrategyType.valueOf(name));
        } catch (Exception e) {

        }

        return null;
    }

    /**
     * 根据类型获取executor分配策略实现类实例
     *
     * @param routeStrategyType executor分配策略类型
     * @return executor分配策略实现类实例
     */
    public static RouteStrategy getByName(RouteStrategyType routeStrategyType) {
        return strategies.get(routeStrategyType);
    }
}
