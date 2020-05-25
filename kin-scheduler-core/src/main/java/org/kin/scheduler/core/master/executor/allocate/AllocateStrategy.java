package org.kin.scheduler.core.master.executor.allocate;

import org.kin.scheduler.core.domain.WorkerRes;
import org.kin.scheduler.core.master.domain.ExecutorRes;
import org.kin.scheduler.core.master.domain.WorkerContext;

import java.util.Collection;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public interface AllocateStrategy {
    /**
     * 分配executor资源
     *
     * @param request        job提交请求
     * @param workerContexts 可用worker信息
     * @return 分配到的executor资源
     */
    List<WorkerRes> allocate(Collection<WorkerContext> workerContexts, Collection<ExecutorRes> usedExecutorReses);
}
