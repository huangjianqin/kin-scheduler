package org.kin.scheduler.core.master.transport;

import org.kin.scheduler.core.transport.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class WorkerUnregisterResult extends RPCResult {
    private static final long serialVersionUID = 7137951714669691528L;

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
