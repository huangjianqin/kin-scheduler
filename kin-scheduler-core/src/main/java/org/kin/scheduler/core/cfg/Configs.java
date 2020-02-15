package org.kin.scheduler.core.cfg;

import org.kin.framework.utils.YamlUtils;

import java.util.Properties;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class Configs {
    public static Config getCfg() {
        Config config = new Config();
        //读取scheduler.yml来获取配置
        Properties workerProperties = YamlUtils.loadYaml2Properties("scheduler.yml");
        //设置值
        for (ConfigKeys cfgKeys : ConfigKeys.KEYS) {
            cfgKeys.set(config, workerProperties);
        }
        return config;
    }
}
