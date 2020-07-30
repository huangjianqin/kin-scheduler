package org.kin.scheduler.core.master.domain;

import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.scheduler.core.driver.ApplicationDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class ApplicationContext {
    /** 应用配置 */
    private ApplicationDescription appDesc;
    /** 已用的 executor资源 */
    private List<ExecutorDesc> executorDescs;
    /** driver 引用 */
    private RpcEndpointRef ref;
    /** executor id计数器 */
    private int executorId;

    public ApplicationContext(ApplicationDescription appDesc, RpcEndpointRef ref) {
        this.appDesc = appDesc;
        this.executorDescs = new CopyOnWriteArrayList<>();
        this.ref = ref;
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 生成唯一executor Id
     */
    private int newExecutorId() {
        return ++executorId;
    }

    /**
     * 占用Executor资源
     *
     * @return 返回executor id
     */
    public String useExecutorResource(WorkerContext worker, int usedCpuCore) {
        String executorId = worker.getWorkerInfo().getWorkerId().concat("-Executor-").concat(String.valueOf(newExecutorId()));
        executorDescs.add(ExecutorDesc.of(executorId, worker, usedCpuCore, 0));
        return executorId;
    }

    /**
     * 是否占用Executor资源
     */
    public boolean containsExecutorResource(String executorId) {
        return executorDescs.stream().anyMatch(r -> r.getExecutorId().equals(executorId));
    }

    /**
     * 释放Executor资源
     */
    public ExecutorDesc removeExecutorResource(String executorId) {
        for (ExecutorDesc executorDesc : new ArrayList<>(executorDescs)) {
            if (executorDesc.getExecutorId().equals(executorId)) {
                executorDescs.remove(executorDesc);
                return executorDesc;
            }
        }
        return null;
    }

    /**
     * @return 需要的cpu核心数
     */
    public int getCpuCoreLeft() {
        return appDesc.getCpuCoreNum() - executorDescs.stream().mapToInt(ExecutorDesc::getUsedCpuCore).sum();
    }

    /**
     * 是否占用Worker资源
     */
    public boolean containsWorkerResource(String workerId) {
        return executorDescs.stream().anyMatch(r -> r.getWorker().getWorkerInfo().getWorkerId().equals(workerId));
    }

    /**
     * worker无效时触发, 释放占用的executor资源
     */
    public List<String> workerUnavailable(String unavailableWorkerId) {
        List<ExecutorDesc> unavailableExecutorDescs = executorDescs.stream()
                .filter(executorDesc -> executorDesc.getWorker().getWorkerInfo().getWorkerId().equals(unavailableWorkerId))
                .collect(Collectors.toList());

        List<String> unavailableExecutorId = new ArrayList<>();
        for (ExecutorDesc unavailableExecutorDesc : unavailableExecutorDescs) {
            executorDescs.remove(unavailableExecutorDesc);

            unavailableExecutorId.add(unavailableExecutorDesc.getExecutorId());
            unavailableExecutorDesc.releaseResources();
        }

        return unavailableExecutorId;
    }

    //------------------------------------------------------------------------------------------------------------------
    public ApplicationDescription getAppDesc() {
        return appDesc;
    }

    public RpcEndpointRef ref() {
        return ref;
    }

    public List<ExecutorDesc> getExecutorDescs() {
        return executorDescs;
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
