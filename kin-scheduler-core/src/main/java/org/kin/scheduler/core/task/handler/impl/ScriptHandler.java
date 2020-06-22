package org.kin.scheduler.core.task.handler.impl;

import ch.qos.logback.classic.Logger;
import org.kin.framework.utils.CommandUtils;
import org.kin.scheduler.core.log.Loggers;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.handler.TaskHandler;
import org.kin.scheduler.core.task.handler.domain.GlueType;
import org.kin.scheduler.core.task.handler.domain.ScriptResourcesStore;
import org.kin.scheduler.core.task.handler.params.ScriptParam;
import org.kin.scheduler.core.task.handler.transport.TaskExecResult;

import java.io.File;
import java.util.Objects;

/**
 * 处理脚本的task handler
 *
 * @author huangjianqin
 * @date 2020-02-21
 */
public class ScriptHandler implements TaskHandler<ScriptParam, TaskExecResult> {
    private static final String RUN_ENCV_PATH = "/runEnv";

    @Override
    public Class<ScriptParam> getTaskParamType() {
        return ScriptParam.class;
    }

    /**
     * 创建脚本文件执行的工作目录
     */
    private static String getOrCreateRealRunEnvPath(String jobId) {
        String realRunEnvPath = RUN_ENCV_PATH.concat(jobId);
        File realRunEnvFile = new File(realRunEnvPath);
        if (!realRunEnvFile.exists()) {
            realRunEnvFile.mkdirs();
        }
        return realRunEnvPath;
    }

    @Override
    public TaskExecResult exec(TaskDescription<ScriptParam> taskDescription) throws Exception {
        //获取logger
        Logger log = Loggers.logger();

        ScriptParam scriptParam = taskDescription.getParam();
        if (Objects.nonNull(scriptParam)) {
            GlueType glueType = GlueType.getByType(scriptParam.getType());
            if (Objects.nonNull(glueType) && glueType.isScript()) {
                //创建环境目录
                String realRunEnvPath = getOrCreateRealRunEnvPath(taskDescription.getJobId());
                String workingDirectory = realRunEnvPath;

                //如果需要脚本资本存储位置
                //否则就是系统已存在bash文件
                ScriptResourcesStore resourcesStore = ScriptResourcesStore.getByName(scriptParam.getScriptResourcesStore());
                if (Objects.nonNull(resourcesStore)) {
                    //复制 ｜ 创建 项目脚本代码
                    if (resourcesStore.equals(ScriptResourcesStore.RESOURCE_CODE)) {
                        //创建脚本文件
                        realRunEnvPath = realRunEnvPath.concat(File.separator).concat(taskDescription.getJobId()).concat(glueType.getSuffix());
                    }
                    resourcesStore.cloneResources(scriptParam.getScriptResources(), scriptParam.getUser(), scriptParam.getPassword(), realRunEnvPath);
                }

                log.info("exec script '{}', params '{}', workingDirectory >>> {}", glueType, scriptParam, workingDirectory);

                int exitValue = CommandUtils.execCommand(scriptParam.getCommand(), Loggers.getTaskOutputFileName(), workingDirectory);
                if (exitValue == 0) {
                    return TaskExecResult.of(exitValue);
                }
            }
        }

        return TaskExecResult.NONE;
    }
}
