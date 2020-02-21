package org.kin.scheduler.core.executor;

import org.kin.framework.utils.SysUtils;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * worker以commandline方式启动Executor
 */
public class ExecutorRunner {
    public static void main(String[] args) {
        if (args.length >= 3) {
            String workerid = args[0];
            String executorId = args[1];
            String backendHost = args[2];
            int backendPort = Integer.valueOf(args[3]);
            int parallelism = SysUtils.CPU_NUM;
            if (args.length >= 5) {
                int parallelismArg = Integer.valueOf(args[4]);
                if (parallelismArg > 0) {
                    parallelism = parallelismArg;
                }
            }
            String logBasePath = args[5];

            runExecutor(workerid, executorId, backendHost, backendPort, parallelism, logBasePath);
        }
    }

    public static void runExecutor(String workerId, String executorId, String backendHost, int backendPort, int parallelism, String logBasePath) {
        Executor executor = new Executor(workerId, executorId, backendHost, backendPort, parallelism, logBasePath);
        try {
            executor.init();
            executor.start();
        } finally {
            executor.close();
        }
    }
}
