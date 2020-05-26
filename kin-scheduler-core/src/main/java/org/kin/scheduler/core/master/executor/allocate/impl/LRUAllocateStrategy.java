package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.collection.LRUMap;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.master.domain.ExecutorResource;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class LRUAllocateStrategy implements AllocateStrategy {
    private static final int EXPIRE_TIME = (int) TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES);

    private Map<String, Boolean> lruMap = new LRUMap<>(19);
    private int monitorTime;

    @Override
    public List<WorkerResource> allocate(Collection<WorkerContext> workerContexts, Collection<ExecutorResource> usedExecutorRese) {
        if (CollectionUtils.isNonEmpty(workerContexts) && CollectionUtils.isEmpty(usedExecutorRese)) {
            synchronized (lruMap) {
                int now = TimeUtils.timestamp();
                if (now >= monitorTime + EXPIRE_TIME) {
                    monitorTime = now;
                    lruMap.clear();
                }

                //put
                Map<String, WorkerContext> workerId2Context = new HashMap<>(workerContexts.size());
                for (WorkerContext workerContext : workerContexts) {
                    workerId2Context.put(workerContext.getWorkerInfo().getWorkerId(), workerContext);
                    lruMap.put(workerContext.getWorkerInfo().getWorkerId(), true);
                }

                //remove invalid
                Set<String> invalidWokerIds = new HashSet<>(lruMap.size());
                for (String workerId : lruMap.keySet()) {
                    if (!workerId2Context.containsKey(workerId)) {
                        invalidWokerIds.add(workerId);
                    }
                }

                for (String workerId : invalidWokerIds) {
                    lruMap.remove(workerId);
                }

                String selectedWorkerId = lruMap.keySet().iterator().next();
                //TODO 目前选择占用全部CPU
                WorkerContext workerContext = workerId2Context.get(selectedWorkerId);
                return Collections.singletonList(new WorkerResource(selectedWorkerId, workerContext.getWorkerInfo().getMaxCpuCore()));
            }
        }

        return null;
    }
}
