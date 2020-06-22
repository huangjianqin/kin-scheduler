package org.kin.scheduler.core.master.executor.allocate;

import org.kin.framework.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * application资源分配策略缓存并
 *
 * @author huangjianqin
 * @date 2020-03-03
 */
public class AllocateStrategies {
    private static Logger log = LoggerFactory.getLogger(AllocateStrategies.class);
    /** key -> 类型, value -> 策略实现 */
    private static Map<AllocateStrategyType, AllocateStrategy> strategies;


    static {
        //加载AllocateStrategies时顺带加载application资源分配策略实现
        Set<Class<? extends AllocateStrategy>> allocateStrategyImplClasses =
                ClassUtils.getSubClass(AllocateStrategy.class.getPackage().getName(), AllocateStrategy.class, false);
        Map<AllocateStrategyType, AllocateStrategy> strategies = new EnumMap<>(AllocateStrategyType.class);
        for (Class<? extends AllocateStrategy> allocateStrategyImplClass : allocateStrategyImplClasses) {
            try {
                String className = allocateStrategyImplClass.getSimpleName();
                int prefix = className.lastIndexOf(AllocateStrategy.class.getSimpleName());
                //XXXXAllocateStrategy类型即为XXXX
                String allocateStrategyName = className.substring(0, prefix);
                AllocateStrategyType allocateStrategyType = AllocateStrategyType.valueOf(allocateStrategyName);
                //获取空构造器
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
