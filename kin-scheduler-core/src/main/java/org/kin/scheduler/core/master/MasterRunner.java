package org.kin.scheduler.core.master;

import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.transport.serializer.SerializerType;
import org.kin.kinrpc.transport.serializer.Serializers;

/**
 * 以commandline方式启动Master
 *
 * @author huangjianqin
 * @date 2020-02-06
 */
public class MasterRunner {
    public static void main(String[] args) {
        if (args.length == 4) {
            String masterHost = args[0];
            int masterPort = Integer.parseInt(args[1]);
            String logPath = args[2];
            int heartbeat = Integer.parseInt(args[3]);

            //创建rpc环境
            RpcEnv rpcEnv = new RpcEnv(masterHost, masterPort, SysUtils.getSuitableThreadNum(),
                    Serializers.getSerializer(SerializerType.KRYO), false);
            //启动server
            rpcEnv.startServer();
            Master master = new Master(rpcEnv, logPath, heartbeat);
            try {
                master.start();
                synchronized (master) {
                    try {
                        master.wait();
                    } catch (InterruptedException e) {

                    }
                }
                master.stop();
            } finally {
                rpcEnv.stop();
            }
        }
    }
}
