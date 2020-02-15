package org.kin.scheduler.core.cfg;

import org.kin.scheduler.core.utils.SchedulerCfgUtils;

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
        public void set(Config config, Properties properties) {
            config.setWorkerBackendHost(SchedulerCfgUtils.getValue(properties, this));
        }
    },
    /**
     *
     */
    WorkerBackendPort("WorkerBackendPort") {
        @Override
        public void set(Config config, Properties properties) {
            config.setWorkerBackendPort(SchedulerCfgUtils.getValue(properties, this));
        }
    },
    /**
     *
     */
    MasterBackendHost("MasterBackendHost") {
        @Override
        public void set(Config config, Properties properties) {
            config.setMasterBackendHost(SchedulerCfgUtils.getValue(properties, this));
        }
    },
    /**
     *
     */
    MasterBackendPort("MasterBackendPort") {
        @Override
        public void set(Config config, Properties properties) {
            config.setMasterBackendPort(SchedulerCfgUtils.getValue(properties, this));
        }
    },
    /**
     * 是否允许worker内置Executor(与Worker共享资源)
     */
    AllowEmbeddedExecutor("是否允许worker内置Executor(与Worker共享资源)") {
        @Override
        public void set(Config config, Properties properties) {
            config.setAllowEmbeddedExecutor(SchedulerCfgUtils.getValue(properties, this));
        }
    },
    /**
     * Executor并发数, 默认等于cpu核数
     */
    Parallelism("Executor并发数") {
        @Override
        public void set(Config config, Properties properties) {
            config.setParallelism(SchedulerCfgUtils.getValue(properties, this));
        }
    },
    /**
     * Executor rpc端口(后续端口是累加上去)
     */
    ExecutorBackendPort("Executor rpc端口") {
        @Override
        public void set(Config config, Properties properties) {
            config.setExecutorBackendPort(SchedulerCfgUtils.getValue(properties, this));
        }
    },
    ;

    public static ConfigKeys[] KEYS = values();

    private String desc;


    ConfigKeys(String desc) {
        this.desc = desc;
    }

    public abstract void set(Config config, Properties properties);
}
