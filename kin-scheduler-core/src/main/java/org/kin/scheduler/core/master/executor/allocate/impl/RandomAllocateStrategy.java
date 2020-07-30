package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机分配资源
 *
 * @author huangjianqin
 * @date 2020-03-03
 */
public class RandomAllocateStrategy implements AllocateStrategy {

    @Override
    public List<WorkerContext> allocate(List<WorkerContext> workers) {
        if (CollectionUtils.isNonEmpty(workers)) {
            return Collections.singletonList(workers.get(ThreadLocalRandom.current().nextInt(workers.size())));
        }

        return null;
    }
}
