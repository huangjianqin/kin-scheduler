package org.kin.scheduler.core.driver.route;

import org.kin.framework.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 加载executor路由策略
 *
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RouteStrategies {
    private static Logger log = LoggerFactory.getLogger(RouteStrategies.class);
    /** key -> executor路由策略类型 value -> 策略实现 */
    private static Map<RouteStrategyType, RouteStrategy> strategies;

    static {
        //加载RouteStrategies时顺带加载executor路由策略实现
        Set<Class<? extends RouteStrategy>> allocateStrategyImplClasses =
                ClassUtils.getSubClass(RouteStrategy.class.getPackage().getName(), RouteStrategy.class, false);
        Map<RouteStrategyType, RouteStrategy> strategies = new EnumMap<>(RouteStrategyType.class);
        for (Class<? extends RouteStrategy> allocateStrategyImplClass : allocateStrategyImplClasses) {
            try {
                String className = allocateStrategyImplClass.getSimpleName();
                //XXXXRouteStrategy类型即为XXXX
                int prefix = className.lastIndexOf(RouteStrategy.class.getSimpleName());
                String allocateStrategyName = className.substring(0, prefix);
                RouteStrategyType routeStrategyType = RouteStrategyType.valueOf(allocateStrategyName);
                //获取空构造器
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
