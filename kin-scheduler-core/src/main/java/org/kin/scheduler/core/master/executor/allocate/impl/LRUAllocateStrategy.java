package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.collection.LRUMap;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 基于LRU(最近最久未使用)的资源分配策略
 * 选择经常使用的
 *
 * @author huangjianqin
 * @date 2020-03-03
 */
public class LRUAllocateStrategy implements AllocateStrategy {
    private static final int EXPIRE_TIME = (int) TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES);

    private Map<String, Boolean> lruMap = new LRUMap<>(19);
    private int monitorTime;

    @Override
    public List<WorkerContext> allocate(List<WorkerContext> workers) {
        if (CollectionUtils.isNonEmpty(workers)) {
            synchronized (lruMap) {
                int now = TimeUtils.timestamp();
                if (now >= monitorTime + EXPIRE_TIME) {
                    monitorTime = now;
                    lruMap.clear();
                }

                //put
                Map<String, WorkerContext> workerId2Context = new HashMap<>(workers.size());
                for (WorkerContext worker : workers) {
                    workerId2Context.put(worker.getWorkerInfo().getWorkerId(), worker);
                    lruMap.put(worker.getWorkerInfo().getWorkerId(), true);
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


                List<Map.Entry<String, Boolean>> entries = new ArrayList<>(lruMap.entrySet());
                if (CollectionUtils.isNonEmpty(entries)) {
                    String selectedWorkerId = entries.get(ThreadLocalRandom.current().nextInt(entries.size())).getKey();
                    return Collections.singletonList(workerId2Context.get(selectedWorkerId));
                }
            }
        }

        return null;
    }
}
