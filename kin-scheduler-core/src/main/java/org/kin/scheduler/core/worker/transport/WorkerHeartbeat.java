package org.kin.scheduler.core.worker.transport;

import org.kin.kinrpc.message.core.RpcEndpointRef;

import java.io.Serializable;

/**
 * worker定时发送给master心跳消息
 *
 * @author huangjianqin
 * @date 2020-05-22
 */
public class WorkerHeartbeat implements Serializable {
    private static final long serialVersionUID = -5853704074373477394L;
    /** 心跳worker id */
    private String workerId;
    /** worker client ref */
    private RpcEndpointRef workerRef;

    public WorkerHeartbeat() {
    }

    public static WorkerHeartbeat of(String workerId, RpcEndpointRef workerRef) {
        WorkerHeartbeat message = new WorkerHeartbeat();
        message.workerId = workerId;
        message.workerRef = workerRef;
        return message;
    }

    //setter && getter
    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public RpcEndpointRef getWorkerRef() {
        return workerRef;
    }

    public void setWorkerRef(RpcEndpointRef workerRef) {
        this.workerRef = workerRef;
    }
}
