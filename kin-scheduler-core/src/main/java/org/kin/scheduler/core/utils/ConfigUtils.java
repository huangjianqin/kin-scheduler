package org.kin.scheduler.core.utils;

import org.kin.framework.utils.StringUtils;

import java.util.Properties;

/**
 * 配置工具类
 *
 * @author huangjianqin
 * @date 2020-02-12
 */
public class ConfigUtils {
    /** 配置key默认前缀 */
    private static final String PREFIX = "kin.scheduler.";

    /**
     * 根据配置enum的name获取配置值
     */
    public static <V> V getValue(Properties properties, Enum e) {
        String key = PREFIX.concat(StringUtils.firstLowerCase(e.name()));
        if (properties.containsKey(key)) {
            return (V) properties.get(key);
        }

        return (V) "";
    }

    /**
     * 是否包含某配置enum
     */
    public static boolean containsKey(Properties properties, Enum e) {
        String key = PREFIX.concat(StringUtils.firstLowerCase(e.name()));

        return properties.containsKey(key);
    }
}
