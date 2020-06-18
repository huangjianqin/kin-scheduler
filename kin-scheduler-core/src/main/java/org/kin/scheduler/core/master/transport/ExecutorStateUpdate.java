package org.kin.scheduler.core.master.transport;

import java.io.Serializable;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-06-16
 */
public class ExecutorStateUpdate implements Serializable {
    private static final long serialVersionUID = -1568064313237525058L;

    private List<String> newExecutorIds;
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
