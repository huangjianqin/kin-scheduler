package org.kin.scheduler.core.master;

import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.transport.serializer.SerializerType;
import org.kin.kinrpc.transport.serializer.Serializers;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class MasterRunner {
    public static void main(String[] args) {
        if (args.length == 4) {
            String masterBackendHost = args[0];
            int masterBackendPort = Integer.parseInt(args[1]);
            String logPath = args[2];
            int heartbeat = Integer.parseInt(args[3]);

            RpcEnv rpcEnv = new RpcEnv(masterBackendHost, masterBackendPort, SysUtils.getSuitableThreadNum(),
                    Serializers.getSerializer(SerializerType.KRYO), false);
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
