package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.master.domain.ExecutorResource;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class LFUAllocateStrategy implements AllocateStrategy {
    private static final int EXPIRE_TIME = (int) TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES);

    private Map<String, Integer> lfuMap = new HashMap<>();
    private int monitorTime;

    @Override
    public List<WorkerResource> allocate(Collection<WorkerContext> workerContexts, Collection<ExecutorResource> usedExecutorRese) {
        if (CollectionUtils.isNonEmpty(workerContexts) && CollectionUtils.isEmpty(usedExecutorRese)) {
            synchronized (lfuMap) {
                int now = TimeUtils.timestamp();
                if (now >= monitorTime + EXPIRE_TIME) {
                    monitorTime = now;
                    lfuMap.clear();
                }

                //put
                Map<String, WorkerContext> workerId2Context = new HashMap<>(workerContexts.size());
                for (WorkerContext workerContext : workerContexts) {
                    String workerId = workerContext.getWorkerInfo().getWorkerId();
                    workerId2Context.put(workerId, workerContext);
                    if (!lfuMap.containsKey(workerId) || lfuMap.get(workerId) > 1000000) {
                        //缓解首次的压力
                        lfuMap.put(workerId, ThreadLocalRandom.current().nextInt(workerContexts.size()));
                    }
                }

                //remove invalid
                Set<String> invalidWokerIds = new HashSet<>(lfuMap.size());
                for (String workerId : lfuMap.keySet()) {
                    if (!workerId2Context.containsKey(workerId)) {
                        invalidWokerIds.add(workerId);
                    }
                }

                for (String workerId : invalidWokerIds) {
                    lfuMap.remove(workerId);
                }

                List<Map.Entry<String, Integer>> entries = new ArrayList<>(lfuMap.entrySet());
                entries.sort(Comparator.comparingInt(Map.Entry::getValue));

                String selectedWorkerId = entries.get(0).getKey();
                lfuMap.put(selectedWorkerId, entries.get(0).getValue() + 1);
                //TODO 目前选择占用全部CPU
                WorkerContext workerContext = workerId2Context.get(selectedWorkerId);
                return Collections.singletonList(new WorkerResource(selectedWorkerId, workerContext.getWorkerInfo().getMaxCpuCore()));
            }
        }
        return null;
    }
}