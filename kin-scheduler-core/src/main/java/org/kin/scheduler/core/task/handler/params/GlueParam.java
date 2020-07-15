package org.kin.scheduler.core.task.handler.params;

import java.io.Serializable;
import java.util.Arrays;

/**
 * glue task参数
 *
 * @author huangjianqin
 * @date 2020-02-21
 */
public class GlueParam implements Serializable {
    protected String command;
    protected String type;
    /** 参数 */
    protected String[] params;

    public static GlueParam of(String command, String type) {
        return GlueParam.of(command, type, new String[0]);
    }

    public static GlueParam of(String command, String type, String[] params) {
        GlueParam glueParam = new GlueParam();
        glueParam.command = command;
        glueParam.type = type;
        glueParam.params = params;
        return glueParam;
    }

    //setter && getter
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "GlueParam{" +
                "command='" + command + '\'' +
                ", type='" + type + '\'' +
                ", params=" + Arrays.toString(params) +
                '}';
    }
}
