package org.kin.scheduler.core.task.handler.params;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public class GlueParam implements Serializable {
    protected String command;
    protected String type;

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

    @Override
    public String toString() {
        return "GlueParam{" +
                "command='" + command + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
