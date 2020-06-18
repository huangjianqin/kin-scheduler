package org.kin.scheduler.core.master.transport;

import org.kin.scheduler.core.transport.RPCResp;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class RegisterWorkerResp extends RPCResp {
    private static final long serialVersionUID = -1527483117146637208L;

    public RegisterWorkerResp() {
    }

    public RegisterWorkerResp(boolean success, String desc) {
        super(success, desc);
    }

    //------------------------------------------------------------------------------------

    public static RegisterWorkerResp success() {
        return success("");
    }

    public static RegisterWorkerResp success(String desc) {
        return new RegisterWorkerResp(true, desc);
    }

    public static RegisterWorkerResp failure(String desc) {
        return new RegisterWorkerResp(false, desc);
    }
}
