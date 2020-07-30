package org.kin.scheduler.core.master.executor.allocate.impl;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.HashUtils;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 基于Hash的资源分配策略
 *
 * @author huangjianqin
 * @date 2020-03-03
 */
public class HashAllocateStrategy implements AllocateStrategy {
    private static final int LIMIT = 9;

    @Override
    public List<WorkerContext> allocate(List<WorkerContext> workers) {
        if (CollectionUtils.isNonEmpty(workers)) {
            TreeMap<Integer, WorkerContext> map = new TreeMap<>();
            for (WorkerContext worker : workers) {
                map.put(HashUtils.efficientHash(worker, LIMIT), worker);
            }

            WorkerContext selected = map.firstEntry().getValue();
            if (Objects.nonNull(selected)) {
                return Collections.singletonList(selected);
            }
        }

        return null;
    }
}
