package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RandomAllocateStrategy implements AllocateStrategy {

    @Override
    public List<WorkerContext> allocate(List<WorkerContext> workerContexts) {
        if (CollectionUtils.isNonEmpty(workerContexts)) {
            return Collections.singletonList(workerContexts.get(ThreadLocalRandom.current().nextInt(workerContexts.size())));
        }

        return null;
    }
}
