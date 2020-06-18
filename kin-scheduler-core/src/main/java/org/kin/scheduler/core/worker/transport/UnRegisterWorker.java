package org.kin.scheduler.core.worker.transport;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-16
 */
public class UnRegisterWorker implements Serializable {
    private static final long serialVersionUID = 1694214116133339948L;

    private String workerId;

    public UnRegisterWorker() {
    }

    public static UnRegisterWorker of(String workerId) {
        UnRegisterWorker message = new UnRegisterWorker();
        message.workerId = workerId;
        return message;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
}
