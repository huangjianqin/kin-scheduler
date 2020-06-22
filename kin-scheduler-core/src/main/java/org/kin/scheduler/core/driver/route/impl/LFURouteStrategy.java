package org.kin.scheduler.core.driver.route.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.scheduler.core.driver.ExecutorContext;
import org.kin.scheduler.core.driver.route.RouteStrategy;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 基于LFU(最近最少使用)的executor路由策略
 * 选择最不常使用的
 *
 * @author huangjianqin
 * @date 2020-03-03
 */
public class LFURouteStrategy implements RouteStrategy {
    private static final int EXPIRE_TIME = (int) TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES);

    private Map<String, Integer> lfuMap = new HashMap<>();
    private int monitorTime;

    @Override
    public ExecutorContext route(Collection<ExecutorContext> availableExecutorContexts) {
        synchronized (lfuMap) {
            int now = TimeUtils.timestamp();
            if (now >= monitorTime + EXPIRE_TIME) {
                monitorTime = now;
                lfuMap.clear();
            }

            //put
            Map<String, ExecutorContext> executorId2Context = new HashMap<>(availableExecutorContexts.size());
            for (ExecutorContext executorContext : availableExecutorContexts) {
                String executorId = executorContext.getExecutorId();
                executorId2Context.put(executorId, executorContext);
                if (!lfuMap.containsKey(executorId) || lfuMap.get(executorId) > 1000000) {
                    //缓解首次的压力
                    lfuMap.put(executorId, ThreadLocalRandom.current().nextInt(availableExecutorContexts.size()));
                }
            }

            //remove invalid
            Set<String> invalidExecutorIds = new HashSet<>(lfuMap.size());
            for (String executorId : lfuMap.keySet()) {
                if (!executorId2Context.containsKey(executorId)) {
                    invalidExecutorIds.add(executorId);
                }
            }

            for (String executorId : invalidExecutorIds) {
                lfuMap.remove(executorId);
            }

            List<Map.Entry<String, Integer>> entries = new ArrayList<>(lfuMap.entrySet());
            if (CollectionUtils.isNonEmpty(entries)) {
                entries.sort(Comparator.comparingInt(Map.Entry::getValue));

                String selectedExecutorrId = entries.get(0).getKey();
                lfuMap.put(selectedExecutorrId, entries.get(0).getValue() + 1);
                return executorId2Context.get(selectedExecutorrId);
            }

            return null;
        }
    }
}
