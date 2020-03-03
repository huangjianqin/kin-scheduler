package org.kin.scheduler.core.master.executor;

import ch.qos.logback.classic.Logger;
import org.kin.framework.utils.ClassUtils;

import java.lang.reflect.Constructor;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class AllocateStrategies {
    private static Logger log;
    private static Map<AllocateStrategyType, AllocateStrategy> strategies;

    static {
        Set<Class<? extends AllocateStrategy>> allocateStrategyImplClasses =
                ClassUtils.getSubClass(AllocateStrategy.class.getPackage().getName(), AllocateStrategy.class, false);
        Map<AllocateStrategyType, AllocateStrategy> strategies = new EnumMap<>(AllocateStrategyType.class);
        for (Class<? extends AllocateStrategy> allocateStrategyImplClass : allocateStrategyImplClasses) {
            try {
                String className = allocateStrategyImplClass.getSimpleName();
                int prefix = className.lastIndexOf(AllocateStrategy.class.getSimpleName());
                String allocateStrategyName = className.substring(0, prefix);
                AllocateStrategyType allocateStrategyType = AllocateStrategyType.valueOf(allocateStrategyName);
                Constructor<? extends AllocateStrategy> constructor = allocateStrategyImplClass.getConstructor();
                strategies.put(allocateStrategyType, constructor.newInstance());
            } catch (Exception e) {
                log.error("init allocate strategy impl class({}) error >>> {}", allocateStrategyImplClass.getName(), e);
            }
        }

        AllocateStrategies.strategies = strategies;
    }

    //----------------------------------------------------------------------------------

    /**
     * 设置日志输出
     */
    public static void setLog(Logger log) {
        AllocateStrategies.log = log;
    }

    /**
     * 根据类型获取executor分配策略实现类实例
     *
     * @param name executor分配策略类型名
     * @return executor分配策略实现类实例
     */
    public static AllocateStrategy getByName(String name) {
        try {
            return strategies.get(AllocateStrategyType.valueOf(name));
        } catch (Exception e) {

        }

        return null;
    }

    /**
     * 根据类型获取executor分配策略实现类实例
     *
     * @param allocateStrategyType executor分配策略类型
     * @return executor分配策略实现类实例
     */
    public static AllocateStrategy getByName(AllocateStrategyType allocateStrategyType) {
        return strategies.get(allocateStrategyType);
    }
}
