package org.kin.scheduler.core.driver;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.transport.serializer.Serializer;
import org.kin.kinrpc.transport.serializer.SerializerType;
import org.kin.kinrpc.transport.serializer.Serializers;
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
    private AllocateStrategyType allocateStrategyType = AllocateStrategyType.Hash;
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
    private int serializerCode = SerializerType.KRYO.getCode();
    /** 通信是否支持压缩 */
    private CompressionType compressionType = CompressionType.NONE;

    public Application() {
    }

    public Application(String appName) {
        this.appName = appName;
    }

    //-----------------------------------------------------------------------------------------------
    public static Application build() {
        return new Application();
    }

    public static Application build(String appName) {
        return new Application(appName);
    }

    //-----------------------------------------------------------------------------------------------

    public Application appName(String appName) {
        this.appName = appName;
        return this;
    }

    public Application master(String masterAddress) {
        this.masterAddress = masterAddress;
        return this;
    }

    public Application master(String masterAddress, AllocateStrategyType allocateStrategyType) {
        master(masterAddress);
        return allocateStrategy(allocateStrategyType);
    }

    public Application driverPort(int driverPort) {
        this.driverPort = driverPort;
        return this;
    }

    public Application allocateStrategy(AllocateStrategyType allocateStrategyType) {
        this.allocateStrategyType = allocateStrategyType;
        return this;
    }

    public Application cpuCore(int cpuCoreNum) {
        this.cpuCoreNum = cpuCoreNum;
        return this;
    }

    public Application minCoresPerExecutor(int minCoresPerExecutor) {
        this.minCoresPerExecutor = minCoresPerExecutor;
        return this;
    }

    public Application oneExecutorPerWorker() {
        this.oneExecutorPerWorker = true;
        return this;
    }

    public Application dropResult() {
        this.dropResult = true;
        return this;
    }

    public Application serializer(String serializerName) {
        Serializer serializer = Serializers.getSerializer(serializerName);
        Preconditions.checkNotNull(String.format("%s serializer must loaded", serializerName));
        this.serializerCode = serializer.type();
        return this;
    }

    public Application serializer(SerializerType serializerType) {
        this.serializerCode = serializerType.getCode();
        return this;
    }

    public Application compression(CompressionType compressionType) {
        this.compressionType = compressionType;
        return this;
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

    public int getSerializerCode() {
        return serializerCode;
    }

    public CompressionType getCompressionType() {
        return compressionType;
    }
}
