package org.kin.scheduler.core.worker.transport;

import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.scheduler.core.worker.domain.WorkerInfo;

import java.io.Serializable;

/**
 * 通知master注册worker消息
 *
 * @author huangjianqin
 * @date 2020-02-07
 */
public class RegisterWorker implements Serializable {
    private static final long serialVersionUID = -9019345657992463377L;
    /** worker信息 */
    private WorkerInfo workerInfo;
    /** worker client ref */
    private RpcEndpointRef workerRef;

    public static RegisterWorker of(WorkerInfo workerInfo, RpcEndpointRef workerRef) {
        RegisterWorker message = new RegisterWorker();
        message.workerInfo = workerInfo;
        message.workerRef = workerRef;
        return message;
    }

    //setter && getter
    public WorkerInfo getWorkerInfo() {
        return workerInfo;
    }

    public void setWorkerInfo(WorkerInfo workerInfo) {
        this.workerInfo = workerInfo;
    }

    public RpcEndpointRef getWorkerRef() {
        return workerRef;
    }

    public void setWorkerRef(RpcEndpointRef workerRef) {
        this.workerRef = workerRef;
    }
}
