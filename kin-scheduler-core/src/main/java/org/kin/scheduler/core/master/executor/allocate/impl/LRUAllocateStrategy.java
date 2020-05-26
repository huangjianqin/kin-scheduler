package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.collection.LRUMap;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.TimeUtils;
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
    public List<WorkerContext> allocate(List<WorkerContext> workerContexts) {
        if (CollectionUtils.isNonEmpty(workerContexts)) {
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

                WorkerContext selectedWorkerContext = workerId2Context.get(selectedWorkerId);
                return Collections.singletonList(selectedWorkerContext);
            }
        }

        return null;
    }
}
