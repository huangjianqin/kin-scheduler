package org.kin.scheduler.core.task.handler.impl;

import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.kin.framework.utils.CommandUtils;
import org.kin.scheduler.core.log.Loggers;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.TaskHandlers;
import org.kin.scheduler.core.task.handler.domain.GlueType;
import org.kin.scheduler.core.task.handler.params.GlueParam;
import org.kin.scheduler.core.task.handler.params.ScriptParam;
import org.kin.scheduler.core.task.handler.transport.TaskExecResult;

import java.util.Objects;

/**
 * glue task handler
 *
 * @author huangjianqin
 * @date 2020-02-21
 */
public class GlueHandler implements TaskHandler<GlueParam, TaskExecResult> {
    @Override
    public Class<GlueParam> getTaskParamType() {
        return GlueParam.class;
    }

    @Override
    public TaskExecResult exec(TaskDescription<GlueParam> taskDescription) throws Exception {
        //获取logger
        Logger log = Loggers.logger();

        GlueParam glueParam = taskDescription.getParam();
        if (Objects.nonNull(glueParam)) {
            //获取glue 类型
            GlueType glueType = GlueType.getByType(glueParam.getType());
            if (Objects.nonNull(glueType)) {
                log.info("exec glue >>> {}, param >>> {}", glueType, glueParam);
                if (glueType.isScript()) {
                    //脚本
                    // fix "\r" in shell
                    if (GlueType.SHELL.equals(glueType)) {
                        ((ScriptParam) glueParam).setScriptResources(((ScriptParam) glueParam).getScriptResources().replaceAll("\r", ""));
                    }

                    //获取task handler
                    TaskHandler taskHandler = TaskHandlers.getTaskHandler(taskDescription);
                    Preconditions.checkNotNull(taskHandler, "task handler is null");

                    return (TaskExecResult) taskHandler.exec(taskDescription);
                } else {
                    //命令
                    int exitValue = CommandUtils.execCommand(glueParam.getCommand(), Loggers.getTaskOutputFileName());
                    if (exitValue == 0) {
                        return TaskExecResult.of(exitValue);
                    }
                }
            }
        }

        return TaskExecResult.NONE;
    }
}
