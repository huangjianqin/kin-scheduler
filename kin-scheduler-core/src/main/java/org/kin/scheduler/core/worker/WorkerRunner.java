package org.kin.scheduler.core.worker;

import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.transport.serializer.SerializerType;
import org.kin.kinrpc.transport.serializer.Serializers;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.cfg.Configs;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * bash
 * 1. 读取scheduler-workers文件获取workerId(一行一个)
 * 2. 读取scheduler.yml来获取配置
 * 3. 批量启动worker
 */
public class WorkerRunner {
    /**
     * args包含一个workerId和worker rpc端口
     */
    public static void main(String[] args) {
        if (args.length == 2) {
            Config config = Configs.getCfg();
            String workerId = args[0];
            config.setWorkerBackendPort(Integer.parseInt(args[1]));

            RpcEnv rpcEnv = new RpcEnv(config.getWorkerBackendHost(), config.getWorkerBackendPort(), SysUtils.getSuitableThreadNum(),
                    Serializers.getSerializer(SerializerType.KRYO), false);
            rpcEnv.startServer();
            Worker worker = new Worker(rpcEnv, workerId, config);
            try {
                worker.start();
                synchronized (worker) {
                    try {
                        worker.wait();
                    } catch (InterruptedException e) {

                    }
                }
                worker.stop();
            } finally {
                rpcEnv.stop();
            }

        }
    }
}
