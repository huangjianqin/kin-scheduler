package org.kin.scheduler.core.task.handler.impl;

import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.task.TaskLoggers;
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.TaskHandlers;
import org.kin.scheduler.core.task.handler.domain.GlueType;
import org.kin.scheduler.core.task.handler.params.GlueParam;
import org.kin.scheduler.core.task.handler.results.GlueResult;
import org.kin.scheduler.core.utils.ScriptUtils;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public class GlueHandler implements TaskHandler<GlueParam, GlueResult> {
    private Logger log = TaskLoggers.logger();

    @Override
    public Class<GlueParam> getTaskParamType() {
        return GlueParam.class;
    }

    @Override
    public GlueResult exec(Task<GlueParam> task) throws Exception {
        GlueParam glueParam = task.getParam();
        if (Objects.nonNull(glueParam)) {
            GlueType glueType = GlueType.getByType(glueParam.getType());
            if (Objects.nonNull(glueType)) {
                log.info("exec glue >>> {}", glueType);
                if (glueType.isScript()) {
                    //获取task handler
                    TaskHandler taskHandler = TaskHandlers.getTaskHandler(task);
                    Preconditions.checkNotNull(taskHandler, "task handler is null");

                    return (GlueResult) taskHandler.exec(task);
                } else {
                    int exitValue = ScriptUtils.execCommand(glueParam.getCommand(), TaskLoggers.getLoggerFile());
                    if (exitValue == 0) {
                        return GlueResult.success();
                    }
                }
            }
        }

        return GlueResult.failure();
    }
}
