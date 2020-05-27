package org.kin.scheduler.core.cfg;

import org.kin.scheduler.core.utils.ConfigUtils;

import java.util.Properties;

/**
 * @author huangjianqin
 * @date 2020-02-08
 * worker通用配置
 */
public enum ConfigKeys {
    /**
     *
     */
    WorkerBackendHost("WorkerBackendHost") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setWorkerBackendHost(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     *
     */
    WorkerBackendPort("WorkerBackendPort") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setWorkerBackendPort(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     *
     */
    MasterBackendHost("MasterBackendHost") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setMasterBackendHost(ConfigUtils.getValue(properties, this));
        }
    },
    /**
     *
     */
    MasterBackendPort("MasterBackendPort") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setMasterBackendPort(ConfigUtils.getValue(properties, this));
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
    ExecutorBackendPort("Executor rpc端口") {
        @Override
        void updateConfig(Config config, Properties properties) {
            config.setExecutorBackendPort(ConfigUtils.getValue(properties, this));
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
    };

    public static ConfigKeys[] KEYS = values();

    private String desc;


    ConfigKeys(String desc) {
        this.desc = desc;
    }

    abstract void updateConfig(Config config, Properties properties);

    public void set(Config config, Properties properties) {
        if (ConfigUtils.containsKey(properties, this)) {
            set(config, properties);
        }
    }
}
