package org.kin.scheduler.core.driver;

import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.scheduler.core.executor.ExecutorBackend;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2020-02-10
 */
public abstract class TaskScheduler extends AbstractService {
    protected Job job;
    private List<ReferenceConfig<ExecutorBackend>> executorBackendReferenceConfigs;
    protected Map<String, ExecutorBackend> executorBackends;
    protected TaskSetManager taskSetManager;

    public TaskScheduler(Job job) {
        super(job.getJobId().concat("-TaskScheduler"));
        this.job = job;
    }

    @Override
    public void init() {
        super.init();
        executorBackendReferenceConfigs = new ArrayList<>();
        executorBackends = new HashMap<>();
        for (Map.Entry<String, String> entry : job.getAvailableExecutors().entrySet()) {
            ReferenceConfig<ExecutorBackend> executorBackendReferenceConfig = References.reference(ExecutorBackend.class)
                    .appName(getName().concat(entry.getKey()))
                    .urls(entry.getValue());
            executorBackends.put(entry.getKey(), executorBackendReferenceConfig.get());
            executorBackendReferenceConfigs.add(executorBackendReferenceConfig);
        }
        taskSetManager = new TaskSetManager();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void close() {
        super.close();
        //取消所有未执行完的task
        if (Objects.nonNull(taskSetManager)) {
            for (TaskContext taskContext : taskSetManager.getAllUnFinishTask()) {
                String execingTaskExecutorId = taskContext.getExecingTaskExecutorId();
                if (StringUtils.isNotBlank(execingTaskExecutorId)) {
                    ExecutorBackend executorBackend = executorBackends.get(execingTaskExecutorId);
                    if (Objects.nonNull(executorBackend)) {
                        executorBackend.cancelTask(taskContext.getTask().getTaskId());
                    }
                }
            }
        }
        if (Objects.nonNull(executorBackendReferenceConfigs)) {
            for (ReferenceConfig<ExecutorBackend> executorBackendReferenceConfig : executorBackendReferenceConfigs) {
                executorBackendReferenceConfig.disable();
            }
        }
    }
}
