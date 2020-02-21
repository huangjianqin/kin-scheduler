package org.kin.scheduler.core.task.handler.impl;

import ch.qos.logback.classic.Logger;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.task.TaskLoggers;
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.domain.GlueType;
import org.kin.scheduler.core.task.handler.domain.ScriptResourcesStore;
import org.kin.scheduler.core.task.handler.domain.Singleton;
import org.kin.scheduler.core.task.handler.params.ScriptParam;
import org.kin.scheduler.core.task.handler.results.GlueResult;
import org.kin.scheduler.core.utils.ScriptUtils;

import java.io.File;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
@Singleton
public class ScriptHandler implements TaskHandler<ScriptParam, GlueResult> {
    private static final String runEnvPath = "/runEnv";

    private Logger log = TaskLoggers.getLogger();

    @Override
    public Class<ScriptParam> getTaskParamType() {
        return ScriptParam.class;
    }

    public static String getOrCreateRealRunEnvPath(String jobId) {
        String realRunEnvPath = runEnvPath.concat(jobId);
        File realRunEnvFile = new File(realRunEnvPath);
        if (!realRunEnvFile.exists()) {
            realRunEnvFile.mkdirs();
        }
        return realRunEnvPath;
    }

    @Override
    public GlueResult exec(Task<ScriptParam> task) throws Exception {
        ScriptParam scriptParam = task.getParam();
        if (Objects.nonNull(scriptParam)) {
            GlueType glueType = GlueType.getByType(scriptParam.getType());
            if (Objects.nonNull(glueType) && glueType.isScript()) {
                ScriptResourcesStore resourcesStore = ScriptResourcesStore.getByName(scriptParam.getScriptResourcesStore());
                if (Objects.nonNull(resourcesStore)) {
                    //复制 ｜ 创建 项目脚本代码
                    String realRunEnvPath = getOrCreateRealRunEnvPath(task.getJobId());
                    String workingDirectory = realRunEnvPath;
                    if (resourcesStore.equals(ScriptResourcesStore.RESOURCE_CODE)) {
                        realRunEnvPath = realRunEnvPath.concat(File.separator).concat(task.getJobId()).concat(glueType.getSuffix());
                    }
                    resourcesStore.cloneResources(scriptParam.getScriptResources(), scriptParam.getUser(), scriptParam.getPassword(), realRunEnvPath);

                    log.info("workingDirectory >>> {}", workingDirectory);

                    int exitValue = ScriptUtils.execCommand(scriptParam.getCommand(), TaskLoggers.getLoggerFile(), workingDirectory);
                    if (exitValue == 0) {
                        return GlueResult.success();
                    }
                }
            }
        }

        return GlueResult.failure();
    }
}
