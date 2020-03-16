package org.kin.scheduler.admin.domain;

import org.kin.framework.utils.JSON;
import org.kin.scheduler.core.task.handler.params.ScriptParam;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-03-08
 */
public enum TaskType {
    /**
     * 打印task
     */
    PRINT(String.class, "打印输出") {
        @Override
        public boolean validParam(String paramJson) {
            return true;
        }
    },
    /**
     * glue task
     */
    GLUE(ScriptParam.class, "GLUE") {
        @Override
        public boolean validParam(String paramJson) {
            return true;
        }
    },
    ;
    public static TaskType[] VALUES = values();
    private Class<? extends Serializable> paramClass;
    private String desc;

    TaskType(Class<? extends Serializable> paramClass, String desc) {
        this.paramClass = paramClass;
        this.desc = desc;
    }

    public static TaskType getByName(String name) {
        for (TaskType taskType : VALUES) {
            if (taskType.name().toLowerCase().equals(name)) {
                return taskType;
            }
        }

        return null;
    }

    public Class<? extends Serializable> getParamClass() {
        return paramClass;
    }

    public String getDesc() {
        return desc;
    }

    public abstract boolean validParam(String paramJson);

    public <P extends Serializable> P parseParam(String paramJson) {
        return (P) JSON.read(paramJson, getParamClass());
    }

}
