package org.kin.scheduler.core.driver;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.kinrpc.serialization.SerializationType;
import org.kin.kinrpc.serialization.Serializations;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategyType;
import org.kin.transport.netty.CompressionType;

/**
 * application配置
 *
 * @author huangjianqin
 * @date 2020-02-11
 */
public class Application {
    /** appName */
    private String appName;
    /** master rpc接口 */
    private String masterAddress = "0.0.0.0:46668";
    /** executor分配策略 */
    private AllocateStrategyType allocateStrategyType = AllocateStrategyType.HASH;
    /** driver rpc服务端口 */
    private int driverPort = 46000;
    /** 需要cpu核心数 */
    private int cpuCoreNum = SysUtils.CPU_NUM;
    /** 每个executor最小需要cpu核心数 */
    private int minCoresPerExecutor = SysUtils.CPU_NUM;
    /** 每个worker一个Executor */
    private boolean oneExecutorPerWorker;
    /** application是否缓存结果 */
    private boolean dropResult;
    /** 通信序列化方式, 默认是kryo */
    private int serializationCode = SerializationType.KRYO.getCode();
    /** 通信是否支持压缩 */
    private CompressionType compressionType = CompressionType.NONE;

    public Application() {
    }

    public Application(String appName) {
        this.appName = appName;
    }

    //getter
    public String getAppName() {
        return appName;
    }

    public String getMasterAddress() {
        return masterAddress;
    }

    public AllocateStrategyType getAllocateStrategyType() {
        return allocateStrategyType;
    }

    public int getDriverPort() {
        return driverPort;
    }

    public int getCpuCoreNum() {
        return cpuCoreNum;
    }

    public int getMinCoresPerExecutor() {
        return minCoresPerExecutor;
    }

    public boolean isOneExecutorPerWorker() {
        return oneExecutorPerWorker;
    }

    public boolean isDropResult() {
        return dropResult;
    }

    public int getSerializationCode() {
        return serializationCode;
    }

    public CompressionType getCompressionType() {
        return compressionType;
    }

    //-----------------------------------------builder------------------------------------------------------
    public static ApplicationBuilder builder() {
        return new ApplicationBuilder();
    }

    public static ApplicationBuilder builder(String appName) {
        return new ApplicationBuilder(appName);
    }

    public static class ApplicationBuilder {
        private Application application;

        public ApplicationBuilder() {
            this("kin-scheduler");
        }

        public Application build() {
            return application;
        }

        public ApplicationBuilder(String appName) {
            this.application = new Application(appName);
        }

        public ApplicationBuilder appName(String appName) {
            application.appName = appName;
            return this;
        }

        public ApplicationBuilder master(String masterAddress) {
            application.masterAddress = masterAddress;
            return this;
        }

        public ApplicationBuilder master(String masterAddress, AllocateStrategyType allocateStrategyType) {
            master(masterAddress);
            return allocateStrategy(allocateStrategyType);
        }

        public ApplicationBuilder driverPort(int driverPort) {
            application.driverPort = driverPort;
            return this;
        }

        public ApplicationBuilder allocateStrategy(AllocateStrategyType allocateStrategyType) {
            application.allocateStrategyType = allocateStrategyType;
            return this;
        }

        public ApplicationBuilder cpuCore(int cpuCoreNum) {
            application.cpuCoreNum = cpuCoreNum;
            return this;
        }

        public ApplicationBuilder minCoresPerExecutor(int minCoresPerExecutor) {
            application.minCoresPerExecutor = minCoresPerExecutor;
            return this;
        }

        public ApplicationBuilder oneExecutorPerWorker() {
            application.oneExecutorPerWorker = true;
            return this;
        }

        public ApplicationBuilder dropResult() {
            application.dropResult = true;
            return this;
        }

        public ApplicationBuilder serialization(String serializationName) {
            Serialization serialization = Serializations.getSerialization(serializationName);
            Preconditions.checkNotNull(String.format("%s serialization must loaded", serializationName));
            application.serializationCode = serialization.type();
            return this;
        }

        public ApplicationBuilder serialization(SerializationType serializationType) {
            application.serializationCode = serializationType.getCode();
            return this;
        }

        public ApplicationBuilder compression(CompressionType compressionType) {
            application.compressionType = compressionType;
            return this;
        }
    }
}
