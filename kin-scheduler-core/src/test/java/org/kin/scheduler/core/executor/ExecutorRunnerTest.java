package org.kin.scheduler.core.executor;

import org.kin.framework.utils.CommandUtils;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.cfg.Configs;

import java.text.MessageFormat;

/**
 * @author huangjianqin
 * @date 2020/8/2
 */
public class ExecutorRunnerTest {
    public static void main(String[] args) throws Exception {
        Config config = Configs.getCfg();
        int cpuCore = 2;
        //JDK 8u191之后
        String command = MessageFormat.format(
                "java -server -XX:ActiveProcessorCount={0} -cp lib/* org.kin.scheduler.core.executor.ExecutorRunner", cpuCore);
        CommandUtils.execCommand(command,
                "", "./",
                "测试", "worker-1", "executor-1", config.getWorkerHost(), String.valueOf(5000),
                config.getLogPath(), "localHost:8080", "localHost:5000",
                config.getSerialize(), Boolean.toString(config.isCompression()));
    }
}
