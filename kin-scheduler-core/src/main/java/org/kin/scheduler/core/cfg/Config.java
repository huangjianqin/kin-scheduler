package org.kin.scheduler.core.cfg;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.kinrpc.serialization.SerializationType;
import org.kin.transport.netty.CompressionType;

/**
 * 配置封装类
 *
 * @author huangjianqin
 * @date 2020-02-08
 */
public class Config {
    /** worker host */
    private String workerHost = "0.0.0.0";
    /** worker port起始值, 因为是根据定义worker的数量, 逐渐递增 */
    private int workerPort;
    /** master host */
    private String masterHost = "0.0.0.0";
    /** master port */
    private int masterPort;
    /** 是否允许worker内置Executor(与Worker共享资源) */
    private boolean allowEmbeddedExecutor;
    /** Executor rpc端口(后续端口是累加上去) */
    private int executorPort;
    /** 日志路径 */
    private String logPath;
    /** 心跳间隔, 默认3s */
    private int heartbeatTime = 3000;
    /** CPU核心数, 默认等于系统cpu核心数 */
    private int cpuCore = SysUtils.CPU_NUM;
    /** 通信序列化方式, 默认是kryo */
    private String serialization = SerializationType.KRYO.name();
    /** 通信是否支持压缩 */
    private CompressionType compressionType = CompressionType.NONE;

    public void check() {
    }

    public int getHeartbeatCheckInterval() {
        return heartbeatTime + 2000;
    }

    public Serialization getSerializationObj() {
        return ExtensionLoader.getExtension(Serialization.class, serialization);
    }

    //setter && getter

    public String getWorkerHost() {
        return workerHost;
    }

    public void setWorkerHost(String workerHost) {
        this.workerHost = workerHost;
    }

    public int getWorkerPort() {
        return workerPort;
    }

    public void setWorkerPort(int workerPort) {
        this.workerPort = workerPort;
    }

    public String getMasterHost() {
        return masterHost;
    }

    public void setMasterHost(String masterHost) {
        this.masterHost = masterHost;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public void setMasterPort(int masterPort) {
        this.masterPort = masterPort;
    }

    public boolean isAllowEmbeddedExecutor() {
        return allowEmbeddedExecutor;
    }

    public void setAllowEmbeddedExecutor(boolean allowEmbeddedExecutor) {
        this.allowEmbeddedExecutor = allowEmbeddedExecutor;
    }

    public int getExecutorPort() {
        return executorPort;
    }

    public void setExecutorPort(int executorPort) {
        this.executorPort = executorPort;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public int getHeartbeatTime() {
        return heartbeatTime;
    }

    public void setHeartbeatTime(int heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }

    public int getCpuCore() {
        return cpuCore;
    }

    public void setCpuCore(int cpuCore) {
        this.cpuCore = cpuCore;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public CompressionType getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(CompressionType compressionType) {
        this.compressionType = compressionType;
    }
}
