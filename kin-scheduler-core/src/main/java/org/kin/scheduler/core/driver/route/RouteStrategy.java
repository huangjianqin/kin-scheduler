package org.kin.scheduler.core.driver.route;

import org.kin.scheduler.core.worker.ExecutorContext;

import java.util.Collection;

/**
 * @author huangjianqin
 * @date 2020-03-03
 */
public interface RouteStrategy {
    /**
     * executor路由
     *
     * @param availableExecutorContexts 可用executor资源
     * @return 根据规则过滤后的可用executor资源
     */
    ExecutorContext route(Collection<ExecutorContext> availableExecutorContexts);
}
