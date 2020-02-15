package org.kin.scheduler.core.executor;

import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * worker以commandline方式启动Executor
 */
public class ExecutorRunner {
    private static final Logger log = LoggerFactory.getLogger(ExecutorRunner.class);

    public static void main(String[] args) {
        if (args.length >= 3) {
            String executorId = args[0];
            String backendHost = args[1];
            int backendPort = Integer.valueOf(args[2]);
            int parallelism = SysUtils.CPU_NUM;
            if (args.length >= 4) {
                int parallelismArg = Integer.valueOf(args[3]);
                if (parallelismArg > 0) {
                    parallelism = parallelismArg;
                }
            }

            runExecutor(executorId, backendHost, backendPort, parallelism);
        }
    }

    public static void runExecutor(String executorId, String backendHost, int backendPort, int parallelism) {
        Executor executor = new Executor(executorId, backendHost, backendPort, parallelism);
        try {
            executor.init();
            executor.start();
        } finally {
            executor.close();
        }
    }
}
