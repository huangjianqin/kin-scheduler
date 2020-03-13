package org.kin.scheduler.admin.core.route.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.admin.core.route.RouteStrategy;
import org.kin.scheduler.core.worker.ExecutorContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RandomRouteStrategy implements RouteStrategy {

    @Override
    public ExecutorContext route(Collection<ExecutorContext> availableExecutorContexts) {
        if (CollectionUtils.isNonEmpty(availableExecutorContexts)) {
            List<ExecutorContext> availableExecutorContextList = new ArrayList<>(availableExecutorContexts);

            Random random = new Random();
            return availableExecutorContextList.get(random.nextInt(availableExecutorContextList.size()));
        }

        return null;
    }
}
