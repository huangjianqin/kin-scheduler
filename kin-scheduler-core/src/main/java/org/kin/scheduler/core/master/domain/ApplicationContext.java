package org.kin.scheduler.core.master.domain;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.driver.ApplicationDescription;
import org.kin.scheduler.core.driver.MasterDriverBackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class ApplicationContext {
    /** 应用配置 */
    private ApplicationDescription appDesc;
    /** 已用的 executor资源 */
    private List<ExecutorResource> usedExecutorResources;
    /** executorDriverBackend url地址 */
    private String executorDriverBackendAddress;

    /** master -> driver 引用 */
    private String masterDriverBackendAddress;
    private ReferenceConfig<MasterDriverBackend> referenceConfig;
    private MasterDriverBackend masterDriverBackend;

    public ApplicationContext(ApplicationDescription appDesc, String executorDriverBackendAddress, String masterDriverBackendAddress) {
        this.appDesc = appDesc;
        this.usedExecutorResources = new CopyOnWriteArrayList<>();
        this.executorDriverBackendAddress = executorDriverBackendAddress;
        this.masterDriverBackendAddress = masterDriverBackendAddress;
    }

    public void init() {
        referenceConfig = References
                .reference(MasterDriverBackend.class)
                .appName("Master".concat("-").concat(appDesc.getAppName()).concat("-").concat("MasterDriverBackend"))
                .urls(masterDriverBackendAddress);
        masterDriverBackend = referenceConfig.get();
    }

    public void stop() {
        referenceConfig.disable();
    }


    public void executorStatusChange(List<String> newExecutorIds, List<String> unavailableExecutorIds) {
        masterDriverBackend.executorStatusChange(newExecutorIds, unavailableExecutorIds);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 占用Executor资源
     */
    public void useExecutorResource(String executorId, WorkerResource resource) {
        usedExecutorResources.add(new ExecutorResource(executorId, resource));
    }

    /**
     * 是否占用Executor资源
     */
    public boolean containsExecutorResource(String executorId) {
        return usedExecutorResources.stream().anyMatch(r -> r.getExecutorId().equals(executorId));
    }

    /**
     * 释放Executor资源
     */
    public ExecutorResource removeExecutorResource(String executorId) {
        for (ExecutorResource executorResource : new ArrayList<>(usedExecutorResources)) {
            if (executorResource.getExecutorId().equals(executorId)) {
                usedExecutorResources.remove(executorResource);
                return executorResource;
            }
        }
        return null;
    }

    /**
     * @return 需要的cpu核心数
     */
    public int getCpuCoreLeft() {
        return appDesc.getCpuCoreNum() - usedExecutorResources.stream().mapToInt(r -> r.getWorkerResource().getCpuCore()).sum();
    }

    /**
     * 是否占用Worker资源
     */
    public boolean containsWorkerResource(String workerId) {
        return usedExecutorResources.stream().anyMatch(r -> r.getWorkerResource().getWorkerId().equals(workerId));
    }

    //getter
    public ApplicationDescription getAppDesc() {
        return appDesc;
    }

    public String getExecutorDriverBackendAddress() {
        return executorDriverBackendAddress;
    }

    public List<ExecutorResource> getUsedExecutorResources() {
        return usedExecutorResources;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApplicationContext that = (ApplicationContext) o;
        return appDesc.equals(that.appDesc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appDesc);
    }
}
