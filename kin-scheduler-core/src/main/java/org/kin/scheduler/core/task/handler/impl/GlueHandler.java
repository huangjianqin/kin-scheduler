package org.kin.scheduler.core.task.handler.impl;

import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.kin.scheduler.core.log.Loggers;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.TaskHandlers;
import org.kin.scheduler.core.task.handler.domain.GlueResult;
import org.kin.scheduler.core.task.handler.domain.GlueType;
import org.kin.scheduler.core.task.handler.params.GlueParam;
import org.kin.scheduler.core.task.handler.params.ScriptParam;
import org.kin.scheduler.core.utils.ScriptUtils;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public class GlueHandler implements TaskHandler<GlueParam, GlueResult> {
    private Logger log = Loggers.logger();

    @Override
    public Class<GlueParam> getTaskParamType() {
        return GlueParam.class;
    }

    @Override
    public GlueResult exec(TaskDescription<GlueParam> taskDescription) throws Exception {
        GlueParam glueParam = taskDescription.getParam();
        if (Objects.nonNull(glueParam)) {
            GlueType glueType = GlueType.getByType(glueParam.getType());
            if (Objects.nonNull(glueType)) {
                log.info("exec glue >>> {}", glueType);
                if (glueType.isScript()) {
                    // fix "\r" in shell
                    if (GlueType.SHELL.equals(glueType)) {
                        ((ScriptParam) glueParam).setScriptResources(((ScriptParam) glueParam).getScriptResources().replaceAll("\r", ""));
                    }

                    //获取task handler
                    TaskHandler taskHandler = TaskHandlers.getTaskHandler(taskDescription);
                    Preconditions.checkNotNull(taskHandler, "task handler is null");

                    return (GlueResult) taskHandler.exec(taskDescription);
                } else {
                    int exitValue = ScriptUtils.execCommand(glueParam.getCommand(), Loggers.getLoggerFileName());
                    if (exitValue == 0) {
                        return GlueResult.success();
                    }
                }
            }
        }

        return GlueResult.failure();
    }
}
