package org.kin.scheduler.core.master.transport;

import org.kin.scheduler.core.transport.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class ApplicationRegisterResponse extends RPCResult {
    private static final long serialVersionUID = -8216922703204452835L;

    public ApplicationRegisterResponse() {
    }

    public ApplicationRegisterResponse(boolean success, String desc) {
        super(success, desc);
    }

    //-------------------------------------------------------

    public static ApplicationRegisterResponse success() {
        return new ApplicationRegisterResponse(true, "");
    }

    public static ApplicationRegisterResponse failure(String desc) {
        return new ApplicationRegisterResponse(false, desc);
    }
}
