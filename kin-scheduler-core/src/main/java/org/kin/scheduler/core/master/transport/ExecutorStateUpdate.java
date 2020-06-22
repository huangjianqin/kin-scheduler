package org.kin.scheduler.core.master.transport;

import java.io.Serializable;
import java.util.List;

/**
 * worker通知master executor状态更新消息
 *
 * @author huangjianqin
 * @date 2020-06-16
 */
public class ExecutorStateUpdate implements Serializable {
    private static final long serialVersionUID = -1568064313237525058L;
    /** 新增executor ids */
    private List<String> newExecutorIds;
    /** 无效executor ids */
    private List<String> unavailableExecutorIds;

    public static ExecutorStateUpdate of(List<String> newExecutorIds, List<String> unavailableExecutorIds) {
        ExecutorStateUpdate message = new ExecutorStateUpdate();
        message.setNewExecutorIds(newExecutorIds);
        message.setUnavailableExecutorIds(unavailableExecutorIds);
        return message;
    }

    public List<String> getNewExecutorIds() {
        return newExecutorIds;
    }

    public void setNewExecutorIds(List<String> newExecutorIds) {
        this.newExecutorIds = newExecutorIds;
    }

    public List<String> getUnavailableExecutorIds() {
        return unavailableExecutorIds;
    }

    public void setUnavailableExecutorIds(List<String> unavailableExecutorIds) {
        this.unavailableExecutorIds = unavailableExecutorIds;
    }
}
