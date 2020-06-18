package org.kin.scheduler.core.driver;

import org.kin.kinrpc.message.core.RpcEndpointRef;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class ExecutorContext {
    private final String workerId;
    private final String executorId;
    private final RpcEndpointRef executorRef;

    public ExecutorContext(String workerId, String executorId, RpcEndpointRef executorRef) {
        this.workerId = workerId;
        this.executorId = executorId;
        this.executorRef = executorRef;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getExecutorId() {
        return executorId;
    }

    public RpcEndpointRef ref() {
        return executorRef;
    }

    public String getExecutorAddress() {
        return executorRef.getEndpointAddress().getRpcAddress().address();
    }

}
