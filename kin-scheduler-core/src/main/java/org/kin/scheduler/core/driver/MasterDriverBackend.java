package org.kin.scheduler.core.driver;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-03-11
 */
public interface MasterDriverBackend {
    void executorStatusChange(List<String> unAvailableExecutorIds);
}
