package org.kin.scheduler.core.master;

import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.cfg.Configs;

/**
 * 以commandline方式启动Master
 *
 * @author huangjianqin
 * @date 2020-02-06
 */
public class MasterRunner {
    public static void main(String[] args) {
        Config config = Configs.getCfg();

        //创建rpc环境
        RpcEnv rpcEnv = new RpcEnv(config.getMasterHost(), config.getMasterPort(), config.getCpuCore(),
                config.getSerializer(), config.isCompression());
        rpcEnv.startServer();
        //启动server
        Master master = new Master(rpcEnv, config);
        try {
            master.createEndpoint();
            synchronized (master) {
                try {
                    JvmCloseCleaner.DEFAULT().add(rpcEnv::stop);
                    master.wait();
                } catch (InterruptedException e) {

                }
            }
        } finally {
            rpcEnv.stop();
        }
    }
}
