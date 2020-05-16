package org.kin.scheduler.core.worker.transport;

import org.kin.scheduler.core.transport.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class WorkerUnregisterResult extends RPCResult {
    public WorkerUnregisterResult() {
    }

    public WorkerUnregisterResult(boolean success, String desc) {
        super(success, desc);
    }

    //------------------------------------------------------------------------------------

    public static WorkerUnregisterResult success() {
        return success("");
    }

    public static WorkerUnregisterResult success(String desc) {
        return new WorkerUnregisterResult(true, desc);
    }

    public static WorkerUnregisterResult failure(String desc) {
        return new WorkerUnregisterResult(false, desc);
    }
}
