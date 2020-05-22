package org.kin.scheduler.core.master.transport;

import org.kin.scheduler.core.transport.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class WorkerRegisterResult extends RPCResult {
    public WorkerRegisterResult() {
    }

    public WorkerRegisterResult(boolean success, String desc) {
        super(success, desc);
    }

    //------------------------------------------------------------------------------------

    public static WorkerRegisterResult success() {
        return success("");
    }

    public static WorkerRegisterResult success(String desc) {
        return new WorkerRegisterResult(true, desc);
    }

    public static WorkerRegisterResult failure(String desc) {
        return new WorkerRegisterResult(false, desc);
    }
}
