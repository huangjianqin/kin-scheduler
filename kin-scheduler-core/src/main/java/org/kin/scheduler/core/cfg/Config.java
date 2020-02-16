package org.kin.scheduler.core.cfg;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class Config {
    private String workerBackendHost;
    //不一定=配置值, 因为是根据定义worker的数量, 逐渐递增
    private int workerBackendPort;
    private String masterBackendHost;
    private int masterBackendPort;
    //是否允许worker内置Executor(与Worker共享资源)
    private boolean allowEmbeddedExecutor;
    //Executor并发数
    private int parallelism;
    //Executor rpc端口(后续端口是累加上去)
    private int executorBackendPort;

    public void check() {
    }

    //setter && getter
    public String getWorkerBackendHost() {
        return workerBackendHost;
    }

    public void setWorkerBackendHost(String workerBackendHost) {
        this.workerBackendHost = workerBackendHost;
    }

    public int getWorkerBackendPort() {
        return workerBackendPort;
    }

    public void setWorkerBackendPort(int workerBackendPort) {
        this.workerBackendPort = workerBackendPort;
    }

    public String getMasterBackendHost() {
        return masterBackendHost;
    }

    public void setMasterBackendHost(String masterBackendHost) {
        this.masterBackendHost = masterBackendHost;
    }

    public int getMasterBackendPort() {
        return masterBackendPort;
    }

    public void setMasterBackendPort(int masterBackendPort) {
        this.masterBackendPort = masterBackendPort;
    }

    public boolean isAllowEmbeddedExecutor() {
        return allowEmbeddedExecutor;
    }

    public void setAllowEmbeddedExecutor(boolean allowEmbeddedExecutor) {
        this.allowEmbeddedExecutor = allowEmbeddedExecutor;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }

    public int getExecutorBackendPort() {
        return executorBackendPort;
    }

    public void setExecutorBackendPort(int executorBackendPort) {
        this.executorBackendPort = executorBackendPort;
    }
}
