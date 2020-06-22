package org.kin.scheduler.core.cfg;

import org.kin.framework.utils.YamlUtils;

import java.util.Properties;

/**
 * 加载配置
 *
 * @author huangjianqin
 * @date 2020-02-13
 */
public class Configs {
    /**
     * 获取配置
     */
    public static Config getCfg() {
        //默认从scheduler.yml获取
        return getCfgFromYml("scheduler.yml");
    }

    /**
     * 从yml读取properties, 并转换成{@link Config}
     *
     * @param path yml文件路径
     */
    public static Config getCfgFromYml(String path) {
        //读取@param path来获取配置
        Properties workerProperties = YamlUtils.loadYaml2Properties(path);
        return transform2Config(workerProperties);
    }

    /**
     * 将properties转换成{@link Config}
     */
    public static Config transform2Config(Properties workerProperties) {
        Config config = new Config();
        //设置值
        for (ConfigKeys cfgKeys : ConfigKeys.KEYS) {
            cfgKeys.set(config, workerProperties);
        }
        return config;
    }
}
