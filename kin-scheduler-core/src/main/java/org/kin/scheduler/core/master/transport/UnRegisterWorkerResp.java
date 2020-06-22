package org.kin.scheduler.core.master.transport;

import org.kin.scheduler.core.transport.RPCResp;

/**
 * worker注销返回消息
 *
 * @author huangjianqin
 * @date 2020-02-08
 */
public class UnRegisterWorkerResp extends RPCResp {
    private static final long serialVersionUID = 7137951714669691528L;

    public static UnRegisterWorkerResp of(boolean success, String desc) {
        UnRegisterWorkerResp resp = new UnRegisterWorkerResp();
        resp.success = success;
        resp.desc = desc;
        return resp;
    }

    public static UnRegisterWorkerResp success() {
        return success("");
    }

    public static UnRegisterWorkerResp success(String desc) {
        return UnRegisterWorkerResp.of(true, desc);
    }

    public static UnRegisterWorkerResp failure(String desc) {
        return UnRegisterWorkerResp.of(false, desc);
    }
}
