package org.kin.scheduler.core.cfg;

import org.kin.scheduler.core.utils.ConfigUtils;

import java.util.Properties;

/**
 * 配置keies
 *
 * @author huangjianqin
 * @date 2020-02-08
 */
public enum ConfigKeys {
    /**
     * worker host
     */
    WorkerHost("WorkerHost") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setWorkerHost(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     * worker port
     */
    WorkerPort("WorkerPort") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setWorkerPort(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     * master host
     */
    MasterHost("MasterHost") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setMasterHost(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     * master port
     */
    MasterPort("MasterPort") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setMasterPort(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     * 是否允许worker内置Executor(与Worker共享资源)
     */
    AllowEmbeddedExecutor("是否允许worker内置Executor(与Worker共享资源)") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setAllowEmbeddedExecutor(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     * Executor rpc端口(后续端口是累加上去)
     */
    ExecutorPort("Executor rpc端口") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setExecutorPort(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     * 日志路径
     */
    LogPath("日志路径") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setLogPath(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     * 心跳间隔
     */
    Heartbeat("心跳间隔") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setHeartbeatTime(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     * CPU core
     */
    CPU("CPU核心数") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setCpuCore(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     * 通信序列化方式
     */
    Serialize("通信序列化方式") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setSerialization(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     * 通信是否支持压缩
     */
    Compression("通信是否支持压缩") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setCompressionType(ConfigUtils.getValue(properties, this));
        }
    };

    public static ConfigKeys[] KEYS = values();
    /** 配置key 描述 */
    private String desc;


    ConfigKeys(String desc) {
        this.desc = desc;
    }

    /**
     * 根据字段更新配置
     *
     * @param config     配置实例
     * @param properties 配置map
     */
    abstract void updateConfig(Config config, Properties properties);

    /**
     * 根据字段设置配置
     * 外部通用接口
     *
     * @param config     配置实例
     * @param properties 配置map
     */
    public void set(Config config, Properties properties) {
        if (ConfigUtils.containsKey(properties, this)) {
            updateConfig(config, properties);
        }
    }
}
