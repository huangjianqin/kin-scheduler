package org.kin.scheduler.core.master.transport;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-05-22
 */
public class WorkerHeartbeatResp implements Serializable {
    private static final long serialVersionUID = -8274141482481478488L;

    public static WorkerHeartbeatResp EMPTY = new WorkerHeartbeatResp();
    public static WorkerHeartbeatResp RECONNECT = reconnectHeartbeatResp();

    private boolean reconnect;

    public static WorkerHeartbeatResp reconnectHeartbeatResp() {
        WorkerHeartbeatResp workerHeartbeatResp = new WorkerHeartbeatResp();
        workerHeartbeatResp.setReconnect(true);
        return workerHeartbeatResp;
    }

    public boolean isReconnect() {
        return reconnect;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }
}
