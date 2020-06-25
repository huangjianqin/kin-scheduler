package org.kin.scheduler.admin.core;

import org.kin.framework.utils.StringUtils;
import org.kin.scheduler.admin.core.domain.TaskInfoDTO;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.admin.utils.MailUtils;
import org.kin.scheduler.core.driver.scheduler.TaskExecCallback;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.task.domain.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * 提交task工具类
 * 单例
 *
 * @author huangjianqin
 * @date 2020-03-08
 */
public class TaskTrigger {
    private static Logger log = LoggerFactory.getLogger(TaskTrigger.class);
    private static TaskTrigger INSTANCE;
    /** task执行回调callback, 单例 */
    private final AdminTaskExecCallback taskSubmitCallback = new AdminTaskExecCallback();

    public static TaskTrigger instance() {
        if (Objects.isNull(INSTANCE)) {
            synchronized (TaskTrigger.class) {
                if (Objects.isNull(INSTANCE)) {
                    INSTANCE = new TaskTrigger();
                }
            }
        }

        return INSTANCE;
    }

    /**
     * 提交task
     *
     * @param taskId task id
     */
    public void trigger(int taskId) {
        TaskInfo taskInfo = KinSchedulerContext.instance().getTaskInfoDao().load(taskId);
        if (Objects.isNull(taskInfo)) {
            log.warn("不存在task");
        }
        trigger(taskInfo);
    }

    /**
     * 提交task
     *
     * @param taskInfo task信息
     */
    public void trigger(TaskInfo taskInfo) {
        trigger(taskInfo.convert());
    }

    /**
     * 提交task
     *
     * @param taskInfoDTO task信息
     */
    private void trigger(TaskInfoDTO taskInfoDTO) {
        //提交task
        TaskExecFuture<Serializable> f = KinSchedulerContext.instance().getDriver().submitTask(taskInfoDTO);
        if (Objects.nonNull(f)) {
            f.addCallback(taskSubmitCallback);
        }
    }

    /**
     * task执行完成回调, 仅仅处理task执行过的情况, 提交失败的情况需要额外处理
     */
    private class AdminTaskExecCallback implements TaskExecCallback<Serializable> {
        private AdminTaskExecCallback() {
        }

        @Override
        public void execFinish(String taskId, TaskStatus taskStatus, Serializable result, String logPath, String outputPath, String reason) {
            //解析出tasklog id
            int index = logPath.lastIndexOf("/");
            if (index < 0) {
                log.error("task({}) log not found >>>>> {}, {}, {}, {}, {}", taskId, taskStatus, result, logPath, outputPath, reason);
                return;
            }
            String taskLogIdStr = logPath.substring(index);
            if (StringUtils.isBlank(taskLogIdStr)) {
                log.error("task({}) log not found >>>>> {}, {}, {}, {}, {}", taskId, taskStatus, result, logPath, outputPath, reason);
                return;
            }

            //获取tasklog
            TaskLog taskLog = KinSchedulerContext.instance().getTaskLogDao().load(Integer.parseInt(taskLogIdStr));
            if (Objects.isNull(taskLog)) {
                log.error("task({}) log not found", taskId);
            }
            if (taskLog.getHandleCode() > 0) {
                log.warn("task({}) log repeate", taskId);
            }
            taskLog.setHandleTime(new Date());
            taskLog.setHandleCode(taskStatus == TaskStatus.FINISHED ? TaskLog.SUCCESS : TaskLog.FAILURE);

            KinSchedulerContext.instance().getTaskLogDao().updateHandleInfo(taskLog);

            boolean isRaiseAlarm = false;
            TaskInfo taskInfo = KinSchedulerContext.instance().getTaskInfoDao().load(Integer.parseInt(taskId));
            if (Objects.nonNull(taskInfo)) {
                if (taskStatus == TaskStatus.FINISHED) {
                    if (StringUtils.isNotBlank(taskInfo.getChildTaskIds())) {
                        String[] childTaskIdStrs = taskInfo.getChildTaskIds().split(",");
                        for (String childTaskIdStr : childTaskIdStrs) {
                            int childTaskId = Integer.parseInt(childTaskIdStr);
                            if (childTaskId > 0) {
                                TaskTrigger.this.trigger(childTaskId);
                            }
                        }
                    }
                } else {
                    //重试
                    tryTriggerAgain(taskLog);
                    isRaiseAlarm = true;
                }
            }

            if (isRaiseAlarm) {
                MailUtils.sendAlarmEmail(taskInfo, taskLog, reason);
            }
        }
    }

    /**
     * task提交失败重试
     */
    private void tryTriggerAgain(TaskLog taskLog) {
        if (taskLog.getRetryTimes() < taskLog.getRetryTimesLimit()) {
            //还有重试次数, 尝试重新提交
            trigger(taskLog.retry());
        } else {
            //没有重试次数
            TaskInfo taskInfo = KinSchedulerContext.instance().getTaskInfoDao().load(taskLog.getTaskId());
            if (Objects.isNull(taskInfo)) {
                return;
            }
            //发送警告邮件
            MailUtils.sendAlarmEmail(taskInfo, taskLog, String.format("task提交重试次数已超过%s次", taskLog.getRetryTimesLimit()));
        }
    }

    /**
     * 提交task失败
     */
    public void submitTaskFail(TaskLog taskLog) {
        taskLog.setHandleCode(TaskLog.FAILURE);
        tryTriggerAgain(taskLog);

        KinSchedulerContext.instance().getTaskLogDao().updateHandleInfo(taskLog);
    }
}
