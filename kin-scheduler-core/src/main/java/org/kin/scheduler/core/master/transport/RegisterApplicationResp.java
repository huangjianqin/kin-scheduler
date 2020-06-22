package org.kin.scheduler.core.master.transport;

import org.kin.scheduler.core.transport.RPCResp;

/**
 * scheduler注册application返回消息
 *
 * @author huangjianqin
 * @date 2020-02-08
 */
public class RegisterApplicationResp extends RPCResp {
    private static final long serialVersionUID = -8216922703204452835L;

    public static RegisterApplicationResp of(boolean success, String desc) {
        RegisterApplicationResp resp = new RegisterApplicationResp();
        resp.success = success;
        resp.desc = desc;
        return resp;
    }

    public static RegisterApplicationResp success() {
        return RegisterApplicationResp.of(true, "");
    }

    public static RegisterApplicationResp failure(String desc) {
        return RegisterApplicationResp.of(false, desc);
    }
}
