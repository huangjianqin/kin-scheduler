package org.kin.scheduler.core.executor;

import org.kin.framework.utils.CommandUtils;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.cfg.Configs;

/**
 * @author huangjianqin
 * @date 2020/8/2
 */
public class ExecutorRunnerTest {
    public static void main(String[] args) throws Exception {
        Config config = Configs.getCfg();
        CommandUtils.execCommand("java -server -cp lib/* org.kin.scheduler.core.executor.ExecutorRunner",
                "", "./",
                "测试", "worker-1", "executor-1", config.getWorkerHost(), String.valueOf(5000),
                config.getLogPath(), "localHost:8080", "localHost:5000",
                config.getSerialize(), Boolean.toString(config.isCompression()));
    }
}
