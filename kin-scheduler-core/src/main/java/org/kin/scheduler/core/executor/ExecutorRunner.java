package org.kin.scheduler.core.executor;

import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.transport.serializer.SerializerType;
import org.kin.kinrpc.transport.serializer.Serializers;
import org.kin.scheduler.core.executor.domain.ExecutorState;

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
            String workerId = args[1];
            String executorId = args[2];
            String backendHost = args[3];
            int backendPort = Integer.valueOf(args[4]);
            String logBasePath = args[5];
            String driverAddress = args[6];
            String workerAddress = args[7];

            RpcEnv rpcEnv = new RpcEnv(backendHost, backendPort, SysUtils.getSuitableThreadNum(),
                    Serializers.getSerializer(SerializerType.KRYO), false);
            rpcEnv.startServer();
            Executor executor = new Executor(rpcEnv, appName, workerId, executorId, logBasePath, driverAddress, workerAddress, false);
            try {
                executor.start();
                synchronized (executor) {
                    try {
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
                executor.stop();
                rpcEnv.stop();
            }
        }
    }
}
