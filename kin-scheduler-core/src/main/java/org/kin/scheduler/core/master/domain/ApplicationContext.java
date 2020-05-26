package org.kin.scheduler.core.master.domain;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.driver.MasterDriverBackend;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class ApplicationContext {
    private String appName;
    private AllocateStrategy allocateStrategy;
    private List<ExecutorResource> usedExecutorResources;
    private String executorDriverBackendAddress;
    /** master -> driver 引用 */
    private String masterDriverBackendAddress;
    private ReferenceConfig<MasterDriverBackend> referenceConfig;
    private MasterDriverBackend masterDriverBackend;

    public ApplicationContext(String appName, AllocateStrategy allocateStrategy, String executorDriverBackendAddress, String masterDriverBackendAddress) {
        this.appName = appName;
        this.allocateStrategy = allocateStrategy;
        this.usedExecutorResources = new CopyOnWriteArrayList<>();
        this.executorDriverBackendAddress = executorDriverBackendAddress;
        this.masterDriverBackendAddress = masterDriverBackendAddress;
    }

    public void init() {
        referenceConfig = References
                .reference(MasterDriverBackend.class)
                .appName("Master".concat("-").concat(appName).concat("-").concat("MasterDriverBackend"))
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

    public void useExecutorResource(String executorId, WorkerResource resource) {
        usedExecutorResources.add(new ExecutorResource(executorId, resource));
    }

    public boolean containsExecutorResource(String executorId) {
        return usedExecutorResources.stream().anyMatch(r -> r.getExecutorId().equals(executorId));
    }

    public ExecutorResource removeExecutorResource(String executorId) {
        Iterator<ExecutorResource> iterator = usedExecutorResources.iterator();
        while (iterator.hasNext()) {
            ExecutorResource executorResource = iterator.next();
            if (executorResource.getExecutorId().equals(executorId)) {
                iterator.remove();
                return executorResource;
            }
        }
        return null;
    }


    //getter
    public String getAppName() {
        return appName;
    }

    public AllocateStrategy getAllocateStrategy() {
        return allocateStrategy;
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
        return Objects.equals(appName, that.appName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName);
    }
}
