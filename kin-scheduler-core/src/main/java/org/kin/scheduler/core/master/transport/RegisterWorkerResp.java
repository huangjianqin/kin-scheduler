package org.kin.scheduler.core.master.transport;

import org.kin.scheduler.core.transport.RPCResp;

/**
 * worker注册返回消息
 *
 * @author huangjianqin
 * @date 2020-02-08
 */
public class RegisterWorkerResp extends RPCResp {
    private static final long serialVersionUID = -1527483117146637208L;

    public static RegisterWorkerResp of(boolean success, String desc) {
        RegisterWorkerResp resp = new RegisterWorkerResp();
        resp.success = success;
        resp.desc = desc;
        return resp;
    }

    public static RegisterWorkerResp success() {
        return success("");
    }

    public static RegisterWorkerResp success(String desc) {
        return RegisterWorkerResp.of(true, desc);
    }

    public static RegisterWorkerResp failure(String desc) {
        return RegisterWorkerResp.of(false, desc);
    }
}
