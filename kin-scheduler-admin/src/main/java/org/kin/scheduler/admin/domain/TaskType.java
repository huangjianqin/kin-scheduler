package org.kin.scheduler.admin.domain;

import org.kin.framework.utils.JSON;
import org.kin.scheduler.core.task.handler.params.GlueParam;
import org.kin.scheduler.core.task.handler.params.ScriptParam;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-03-08
 */
public enum TaskType {
    /**
     * glue task
     */
    GLUE(GlueParam.class, "GLUE") {
        @Override
        public boolean validParam(String paramJson) {
            return true;
        }
    },
    /**
     * script task
     */
    SCRIPT(ScriptParam.class, "脚本") {
        @Override
        public boolean validParam(String paramJson) {
            return true;
        }
    },
    ;
    public static TaskType[] VALUES = values();
    /** task类型对应的参数类型 */
    private Class<? extends Serializable> paramClass;
    /** task类型描述 */
    private String desc;

    TaskType(Class<? extends Serializable> paramClass, String desc) {
        this.paramClass = paramClass;
        this.desc = desc;
    }

    /**
     * 根据类型名获取类型实例
     *
     * @param name 类型名
     * @return 类型实例
     */
    public static TaskType getByName(String name) {
        for (TaskType taskType : VALUES) {
            if (taskType.name().toLowerCase().equals(name.toLowerCase())) {
                return taskType;
            }
        }

        return null;
    }

    /**
     * 根据类型名获取类型描述
     *
     * @param name 类型名
     * @return 类型实例
     */
    public static String getDescByName(String name) {
        TaskType taskType = getByName(name);
        return Objects.nonNull(taskType) ? taskType.desc : "unknown: ".concat(name);
    }

    public Class<? extends Serializable> getParamClass() {
        return paramClass;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 校验参数是否符合预期参数类型
     */
    public abstract boolean validParam(String paramJson);

    /**
     * 反序列化出参数实例
     */
    public <P extends Serializable> P parseParam(String paramJson) {
        return (P) JSON.read(paramJson, getParamClass());
    }

}
