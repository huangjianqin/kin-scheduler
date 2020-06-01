package org.kin.scheduler.core.task.handler.domain;

import org.kin.framework.utils.StringUtils;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public enum GlueType {
    /**
     * linux 命令
     */
    BASH("BASH", false, null, null),
    /**
     * .sh脚本
     */
    SHELL("GLUE(Shell)", true, "/bin/bash", ".sh"),
    /**
     * python脚本
     */
    PYTHON("GLUE(Python)", true, "python", ".py"),
    /**
     * js脚本
     */
    NODEJS("GLUE(Nodejs)", true, "node", ".js"),
    ;
    public static GlueType[] TYPES = values();

    private String desc;
    private boolean isScript;
    private String cmd;
    private String suffix;

    GlueType(String desc, boolean isScript, String cmd, String suffix) {
        this.desc = desc;
        this.isScript = isScript;
        this.cmd = cmd;
        this.suffix = suffix;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isScript() {
        return isScript;
    }

    public String getCmd() {
        return cmd;
    }

    public String getSuffix() {
        return suffix;
    }

    //----------------------------------------------------------------------------------

    public static GlueType getByType(String type) {
        if (StringUtils.isNotBlank(type)) {
            for (GlueType item : TYPES) {
                if (item.name().toLowerCase().equals(type.toLowerCase())) {
                    return item;
                }
            }
        }
        return null;
    }
}
