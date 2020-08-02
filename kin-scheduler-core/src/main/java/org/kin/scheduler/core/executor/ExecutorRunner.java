package org.kin.scheduler.core.executor;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.transport.serializer.Serializers;
import org.kin.scheduler.core.executor.domain.ExecutorState;

/**
 * worker以commandline方式启动Executor
 *
 * @author huangjianqin
 * @date 2020-02-06
 */
public class ExecutorRunner {
    public static void main(String[] args) {
        if (args.length >= 10) {
            String appName = args[0];
            String workerId = args[1];
            String executorId = args[2];
            String host = args[3];
            int port = Integer.parseInt(args[4]);
            String logBasePath = args[5];
            String driverAddress = args[6];
            String workerAddress = args[7];
            String serialize = args[8];
            boolean compression = Boolean.parseBoolean(args[9]);

            //创建rpc env
            //外部进程通过commandline方式限制进程能使用的cpu核心数
            RpcEnv rpcEnv = new RpcEnv(host, port, SysUtils.getSuitableThreadNum(), Serializers.getSerializer(serialize), compression);
            //启动server
            rpcEnv.startServer();
            //创建executor
            Executor executor = new Executor(rpcEnv, appName, workerId, executorId, logBasePath, driverAddress, workerAddress, false);
            try {
                executor.createEndpoint();
                synchronized (executor) {
                    try {
                        JvmCloseCleaner.DEFAULT().add(rpcEnv::stop);
                        executor.wait();
                    } catch (InterruptedException e) {
                        throw e;
                    }
                }
                //更新状态executor
                executor.executorStateChanged(ExecutorState.EXIT);
            } catch (InterruptedException e) {
                //更新状态executor
                executor.executorStateChanged(ExecutorState.KILLED);
            } catch (Exception e) {
                //更新状态executor
                executor.executorStateChanged(ExecutorState.FAIL);
            } finally {
                //executor stop
                rpcEnv.stop();
            }
        }
    }
}
