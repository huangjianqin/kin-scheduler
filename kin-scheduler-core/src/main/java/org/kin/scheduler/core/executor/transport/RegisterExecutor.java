package org.kin.scheduler.core.executor.transport;

import org.kin.kinrpc.message.core.RpcEndpointRef;

import java.io.Serializable;

/**
 * executor注册消息
 *
 * @author huangjianqin
 * @date 2020-03-09
 */
public class RegisterExecutor implements Serializable {
    private static final long serialVersionUID = 5953742842842847510L;
    /** worker id */
    private String workerId;
    /** executor id */
    private String executorId;
    /** executor client ref */
    private RpcEndpointRef executorRef;

    public static RegisterExecutor of(String workerId, String executorId, RpcEndpointRef executorRef) {
        RegisterExecutor message = new RegisterExecutor();
        message.workerId = workerId;
        message.executorId = executorId;
        message.executorRef = executorRef;
        return message;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getExecutorId() {
        return executorId;
    }

    public void setExecutorId(String executorId) {
        this.executorId = executorId;
    }

    public RpcEndpointRef getExecutorRef() {
        return executorRef;
    }

    public void setExecutorRef(RpcEndpointRef executorRef) {
        this.executorRef = executorRef;
    }
}
