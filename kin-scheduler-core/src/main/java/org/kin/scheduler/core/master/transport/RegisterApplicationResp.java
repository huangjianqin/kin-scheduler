package org.kin.scheduler.core.master.transport;

import org.kin.scheduler.core.transport.RPCResp;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class RegisterApplicationResp extends RPCResp {
    private static final long serialVersionUID = -8216922703204452835L;

    public RegisterApplicationResp() {
    }

    public RegisterApplicationResp(boolean success, String desc) {
        super(success, desc);
    }

    //-------------------------------------------------------

    public static RegisterApplicationResp success() {
        return new RegisterApplicationResp(true, "");
    }

    public static RegisterApplicationResp failure(String desc) {
        return new RegisterApplicationResp(false, desc);
    }
}
