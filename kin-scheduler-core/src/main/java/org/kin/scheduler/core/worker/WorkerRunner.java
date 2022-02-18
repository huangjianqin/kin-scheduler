package org.kin.scheduler.core.worker;

import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.cfg.Configs;

/**
 * 以commandline形式运行worker
 * 1. 读取scheduler-workers文件获取workerId(一行一个)
 * 2. 读取scheduler.yml来获取配置
 * 3. 批量启动worker
 *
 * @author huangjianqin
 * @date 2020-02-06
 */
public class WorkerRunner {
    /**
     * args包含一个workerId和worker rpc端口
     */
    public static void main(String[] args) {
        if (args.length == 2) {
            Config config = Configs.getCfg();
            String workerId = args[0];
            config.setWorkerPort(Integer.parseInt(args[1]));

            //创建rpc环境
            RpcEnv rpcEnv = new RpcEnv(config.getWorkerHost(), config.getWorkerPort(), config.getCpuCore(),
                    config.getSerializationObj(), config.getCompressionType());
            //启动server
            rpcEnv.startServer();
            Worker worker = new Worker(rpcEnv, workerId, config);
            try {
                worker.createEndpoint();
                synchronized (worker) {
                    try {
                        JvmCloseCleaner.instance().add(rpcEnv::stop);
                        worker.wait();
                    } catch (InterruptedException e) {

                    }
                }
            } finally {
                rpcEnv.stop();
            }

        }
    }
}
