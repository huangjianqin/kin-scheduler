package org.kin.scheduler.core.master.domain;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.scheduler.core.driver.MasterDriverBackend;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class ApplicationContext {
    private String appName;
    private AllocateStrategy allocateStrategy;
    private List<ExecutorRes> usedExecutorReses;
    private String executorDriverBackendAddress;
    private String masterDriverBackendAddress;
    private ReferenceConfig<MasterDriverBackend> referenceConfig;
    private MasterDriverBackend masterDriverBackend;

    public ApplicationContext(String appName, AllocateStrategy allocateStrategy, String executorDriverBackendAddress, String masterDriverBackendAddress) {
        this.appName = appName;
        this.allocateStrategy = allocateStrategy;
        this.usedExecutorReses = new CopyOnWriteArrayList<>();
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


    public void executorStatusChange(List<ExecutorRes> newExecutorReses, List<String> unavailableExecutorIds) {
        usedExecutorReses.removeIf(item -> unavailableExecutorIds.contains(item.getExecutorId()));
        usedExecutorReses.addAll(newExecutorReses);

        List<String> newExecutorIds = newExecutorReses.stream().map(ExecutorRes::getExecutorId).collect(Collectors.toList());
        masterDriverBackend.executorStatusChange(newExecutorIds, unavailableExecutorIds);
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

    public List<ExecutorRes> getUsedExecutorReses() {
        return usedExecutorReses;
    }

    public void setUsedExecutorReses(List<ExecutorRes> usedExecutorReses) {
        this.usedExecutorReses = usedExecutorReses;
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
