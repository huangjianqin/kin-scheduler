package org.kin.scheduler.admin.core.route.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.admin.core.route.RouteStrategy;
import org.kin.scheduler.core.worker.ExecutorContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RoundRobinRouteStrategy implements RouteStrategy {
    private AtomicInteger round = new AtomicInteger(0);

    @Override
    public ExecutorContext route(Collection<ExecutorContext> availableExecutorContexts) {
        if (CollectionUtils.isNonEmpty(availableExecutorContexts)) {
            List<ExecutorContext> availableExecutorContextList = new ArrayList<>(availableExecutorContexts);
            return availableExecutorContextList.get(next(availableExecutorContextList.size()));
        }

        return null;
    }

    private int next(int size) {
        if (round.get() <= 0) {
            round.set(0);
        }
        return (round.getAndAdd(1) + size) % size;
    }
}
