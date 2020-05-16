package org.kin.scheduler.core.task.handler.domain;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public class GlueResult extends HandlerExecResult {
    public GlueResult() {
    }

    public GlueResult(boolean success) {
        super(success);
    }
    //----------------------------------------------------------------------------------------

    public static GlueResult success() {
        return new GlueResult(true);
    }

    public static GlueResult failure() {
        return new GlueResult(false);
    }
}
