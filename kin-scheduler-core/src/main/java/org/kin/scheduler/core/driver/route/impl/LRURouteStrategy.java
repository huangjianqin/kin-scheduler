package org.kin.scheduler.core.driver.route.impl;

import org.kin.framework.collection.LRUMap;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.scheduler.core.driver.ExecutorContext;
import org.kin.scheduler.core.driver.route.RouteStrategy;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 基于LRU(最近最久未使用)的executor路由策略
 * 选择经常使用的
 *
 * @author huangjianqin
 * @date 2020-03-03
 */
public class LRURouteStrategy implements RouteStrategy {
    private static final int EXPIRE_TIME = (int) TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES);

    private Map<String, Boolean> lruMap = new LRUMap<>(19);
    private int monitorTime;

    @Override
    public ExecutorContext route(Collection<ExecutorContext> availableExecutorContexts) {
        synchronized (lruMap) {
            int now = TimeUtils.timestamp();
            if (now >= monitorTime + EXPIRE_TIME) {
                monitorTime = now;
                lruMap.clear();
            }

            //put
            Map<String, ExecutorContext> executorId2Context = new HashMap<>(availableExecutorContexts.size());
            for (ExecutorContext executorContext : availableExecutorContexts) {
                executorId2Context.put(executorContext.getExecutorId(), executorContext);
                lruMap.put(executorContext.getExecutorId(), true);
            }

            //remove invalid
            Set<String> invalidExecutorIds = new HashSet<>(lruMap.size());
            for (String executorId : lruMap.keySet()) {
                if (!executorId2Context.containsKey(executorId)) {
                    invalidExecutorIds.add(executorId);
                }
            }

            for (String executorId : invalidExecutorIds) {
                lruMap.remove(executorId);
            }

            List<Map.Entry<String, Boolean>> entries = new ArrayList<>(lruMap.entrySet());
            if (CollectionUtils.isNonEmpty(entries)) {
                String selectedWorkerId = entries.get(ThreadLocalRandom.current().nextInt(entries.size())).getKey();
                return executorId2Context.get(selectedWorkerId);
            }

            return null;
        }
    }
}
