package org.kin.scheduler.core.driver.route.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.driver.route.RouteStrategy;
import org.kin.scheduler.core.worker.ExecutorContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RandomRouteStrategy implements RouteStrategy {

    @Override
    public ExecutorContext route(Collection<ExecutorContext> availableExecutorContexts) {
        if (CollectionUtils.isNonEmpty(availableExecutorContexts)) {
            List<ExecutorContext> availableExecutorContextList = new ArrayList<>(availableExecutorContexts);

            return availableExecutorContextList.get(ThreadLocalRandom.current().nextInt(availableExecutorContextList.size()));
        }

        return null;
    }
}
