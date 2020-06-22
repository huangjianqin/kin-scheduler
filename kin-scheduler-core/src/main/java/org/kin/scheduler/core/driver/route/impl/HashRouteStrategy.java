package org.kin.scheduler.core.driver.route.impl;

import org.kin.framework.utils.HashUtils;
import org.kin.scheduler.core.driver.ExecutorContext;
import org.kin.scheduler.core.driver.route.RouteStrategy;

import java.util.Collection;
import java.util.TreeMap;

/**
 * 基于hash的executor路由策略
 *
 * @author huangjianqin
 * @date 2020-03-03
 */
public class HashRouteStrategy implements RouteStrategy {
    private static final int LIMIT = 9;

    @Override
    public ExecutorContext route(Collection<ExecutorContext> availableExecutorContexts) {
        TreeMap<Integer, ExecutorContext> map = new TreeMap<>();
        for (ExecutorContext executorContext : availableExecutorContexts) {
            map.put(HashUtils.efficientHash(executorContext, LIMIT), executorContext);
        }

        return map.firstEntry().getValue();
    }
}
