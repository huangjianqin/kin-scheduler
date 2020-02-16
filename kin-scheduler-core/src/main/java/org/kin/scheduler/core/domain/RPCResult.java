package org.kin.scheduler.core.domain;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class RPCResult implements Serializable {
    //执行结果
    protected boolean success;
    //结果描述
    protected String desc;

    public RPCResult() {
    }

    public RPCResult(boolean success) {
        this(success, "");
    }

    public RPCResult(boolean success, String desc) {
        this.success = success;
        this.desc = desc;
    }

    //-------------------------------------------------------------------------------------
    public static RPCResult success() {
        return success("");
    }

    public static RPCResult success(String desc) {
        return new RPCResult(true, desc);
    }

    public static RPCResult failure(String desc) {
        return new RPCResult(false, desc);
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
