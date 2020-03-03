package org.kin.scheduler.core.executor;

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
            String logBasePath = args[4];

            runExecutor(workerid, executorId, backendHost, backendPort, logBasePath);
        }
    }

    public static void runExecutor(String workerId, String executorId, String backendHost, int backendPort, String logBasePath) {
        Executor executor = new Executor(workerId, executorId, backendHost, backendPort, logBasePath);
        try {
            executor.init();
            executor.start();
        } finally {
            executor.close();
        }
    }
}
