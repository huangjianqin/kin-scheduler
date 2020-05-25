package org.kin.scheduler.core.executor;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * worker以commandline方式启动Executor
 */
public class ExecutorRunner {
    public static void main(String[] args) {
        if (args.length >= 8) {
            String appName = args[0];
            String workerid = args[1];
            String executorId = args[2];
            String backendHost = args[3];
            int backendPort = Integer.valueOf(args[4]);
            String logBasePath = args[5];
            String driverAddress = args[6];
            String workerAddress = args[7];

            runExecutor(appName, workerid, executorId, backendHost, backendPort, logBasePath, driverAddress, workerAddress);
        }
    }

    private static void runExecutor0(Executor executor) {
        try {
            executor.init();
            executor.start();
            synchronized (executor) {
                try {
                    executor.wait();
                } catch (InterruptedException e) {
                    throw e;
                }
            }
            //TODO 更新状态executor
        } catch (Exception e) {
            //TODO 更新状态executor
        } finally {
            executor.close();
        }
    }

    public static void runExecutor(String appName, String workerId, String executorId, String backendHost, int backendPort,
                                   String logBasePath, String driverAddress, String workerAddress) {
        Executor executor = new Executor(appName, workerId, executorId, backendHost, backendPort, logBasePath, driverAddress, workerAddress);
        runExecutor0(executor);
    }
}
