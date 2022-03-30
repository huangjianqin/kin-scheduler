package org.kin.scheduler.core.executor;

import com.google.common.base.Preconditions;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.scheduler.core.executor.domain.ExecutorState;
import org.kin.transport.netty.CompressionType;

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
            String serializationName = args[8];

            Serialization serialization = ExtensionLoader.getExtension(Serialization.class, serializationName);

            int compressionTypeId = Integer.parseInt(args[9]);
            CompressionType compressionType = CompressionType.getById(compressionTypeId);

            Preconditions.checkNotNull(serialization, String.format("can't find Serialization with type = %s", serializationName));
            Preconditions.checkNotNull(compressionType, String.format("can't find CompressionType with id = %s", compressionTypeId));

            //创建rpc env
            //外部进程通过commandline方式限制进程能使用的cpu核心数
            RpcEnv rpcEnv = new RpcEnv(host, port, SysUtils.getSuitableThreadNum(), serialization, compressionType);
            //启动server
            rpcEnv.startServer();
            //创建executor
            Executor executor = new Executor(rpcEnv, appName, workerId, executorId, logBasePath, driverAddress, workerAddress, false);
            try {
                executor.createEndpoint();
                synchronized (executor) {
                    try {
                        JvmCloseCleaner.instance().add(rpcEnv::stop);
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
        } else {
            System.err.println("参数有误!!!");
            StringBuffer helpStrBuff = new StringBuffer();
            helpStrBuff.append("参数1: ").append("应用ming");
            helpStrBuff.append("参数2: ").append("workerId");
            helpStrBuff.append("参数3: ").append("executorId");
            helpStrBuff.append("参数4: ").append("绑定的host");
            helpStrBuff.append("参数5: ").append("绑定的端口");
            helpStrBuff.append("参数6: ").append("日志base路径");
            helpStrBuff.append("参数7: ").append("driver远程连接地址");
            helpStrBuff.append("参数8: ").append("worker远程连接地址");
            helpStrBuff.append("参数9: ").append("序列化方式");
            helpStrBuff.append("参数10: ").append("通信是否支持压缩");
            System.err.println(helpStrBuff.toString());
        }
    }
}
