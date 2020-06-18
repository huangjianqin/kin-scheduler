package org.kin.scheduler.core.transport;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class RPCResp implements Serializable {
    private static final long serialVersionUID = 5197954993271710582L;

    /** 执行结果 */
    protected boolean success;
    /** 结果描述 */
    protected String desc;

    public RPCResp() {
    }

    public RPCResp(boolean success) {
        this(success, "");
    }

    public RPCResp(boolean success, String desc) {
        this.success = success;
        this.desc = desc;
    }

    //-------------------------------------------------------------------------------------

    public static RPCResp success() {
        return success("");
    }

    public static RPCResp success(String desc) {
        return new RPCResp(true, desc);
    }

    public static RPCResp failure(String desc) {
        return new RPCResp(false, desc);
    }

    //-------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "RPCResult{" +
                "success=" + success +
                ", desc='" + desc + '\'' +
                '}';
    }

    //setter && getter

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
