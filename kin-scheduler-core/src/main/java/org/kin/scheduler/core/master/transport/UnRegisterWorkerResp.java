package org.kin.scheduler.core.master.transport;

import org.kin.scheduler.core.transport.RPCResp;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class UnRegisterWorkerResp extends RPCResp {
    private static final long serialVersionUID = 7137951714669691528L;

    public UnRegisterWorkerResp() {
    }

    public UnRegisterWorkerResp(boolean success, String desc) {
        super(success, desc);
    }

    //------------------------------------------------------------------------------------

    public static UnRegisterWorkerResp success() {
        return success("");
    }

    public static UnRegisterWorkerResp success(String desc) {
        return new UnRegisterWorkerResp(true, desc);
    }

    public static UnRegisterWorkerResp failure(String desc) {
        return new UnRegisterWorkerResp(false, desc);
    }
}
