package org.kin.scheduler.core.utils;

import org.kin.framework.utils.StringUtils;

import java.util.Properties;

/**
 * @author huangjianqin
 * @date 2020-02-12
 */
public class SchedulerCfgUtils {
    private static final String PREFIX = "kin.scheduler.";

    public static <V> V getValue(Properties properties, Enum e) {
        String key = PREFIX.concat(StringUtils.firstLowerCase(e.name()));
        if (properties.containsKey(key)) {
            return (V) properties.get(key);
        }

        return (V) "";
    }
}
