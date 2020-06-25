package org.kin.scheduler.admin.service.impl;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.web.domain.WebResponse;
import org.kin.scheduler.admin.core.KinSchedulerContext;
import org.kin.scheduler.admin.dao.JobInfoDao;
import org.kin.scheduler.admin.dao.TaskInfoDao;
import org.kin.scheduler.admin.dao.TaskLogDao;
import org.kin.scheduler.admin.domain.TaskType;
import org.kin.scheduler.admin.domain.TimeType;
import org.kin.scheduler.admin.entity.JobInfo;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.admin.entity.User;
import org.kin.scheduler.admin.service.JobService;
import org.kin.scheduler.admin.service.UserService;
import org.kin.scheduler.admin.utils.MailUtils;
import org.kin.scheduler.admin.vo.TaskInfoVO;
import org.kin.scheduler.core.driver.route.RouteStrategyType;
import org.kin.scheduler.core.task.TaskExecStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@Service
public class JobServiceImpl implements JobService {
    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    @Autowired
    private JobInfoDao jobInfoDao;
    @Autowired
    private TaskInfoDao taskInfoDao;
    @Autowired
    private TaskLogDao taskLogDao;
    @Autowired
    private UserService userService;

    @Override
    public Map<String, Object> pageList(int start, int length, int jobId, int triggerStatus, String desc, String type, String userName) {
        // page list
        List<TaskInfoVO> list = taskInfoDao.pageList(start, length, jobId, triggerStatus, desc, type, userName);
        int listCount = taskInfoDao.pageListCount(start, length, jobId, triggerStatus, desc, type, userName);

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        // 总记录数
        maps.put("num", listCount);
        // 分页列表
        maps.put("data", list);
        return maps;
    }

    @Override
    public WebResponse<String> add(TaskInfo taskInfo) {
        WebResponse<String> validWebResponse = validTaskInfo(taskInfo);
        if (Objects.nonNull(validWebResponse)) {
            return validWebResponse;
        }

        Date now = new Date();
        taskInfo.setAddTime(now);
        taskInfo.setUpdateTime(now);
        //清掉触发状态
        taskInfo.stop();

        taskInfoDao.save(taskInfo);
        if (taskInfo.getId() < 1) {
            return WebResponse.fail("db error");
        }

        return WebResponse.success(String.valueOf(taskInfo.getId()));
    }

    /**
     * 校验task信息是否有误
     *
     * @param taskInfo task信息
     */
    private WebResponse<String> validTaskInfo(TaskInfo taskInfo) {
        // valid
        JobInfo job = jobInfoDao.load(taskInfo.getJobId());
        if (Objects.nonNull(job)) {
            return WebResponse.fail("不存在作业");
        }

        try {
            TimeType timeType = TimeType.getByName(taskInfo.getTimeType());
            if (!timeType.validTimeFormat(taskInfo.getTimeStr())) {
                return WebResponse.fail("时间格式错误");
            }
        } catch (Exception e) {
            return WebResponse.fail("时间格式错误");
        }

        if (StringUtils.isBlank(taskInfo.getDesc())) {
            return WebResponse.fail("task描述为空");
        }
        if (Objects.isNull(TaskExecStrategy.getByName(taskInfo.getExecStrategy()))) {
            return WebResponse.fail("未知task执行策略");
        }
        if (Objects.isNull(RouteStrategyType.getByName(taskInfo.getRouteStrategy()))) {
            return WebResponse.fail("未知task executor分配策略");
        }
        TaskType taskType = TaskType.getByName(taskInfo.getParam());
        if (Objects.isNull(taskType) || !taskType.validParam(taskInfo.getParam())) {
            return WebResponse.fail("未知task类型及参数");
        }

        // ChildTaskId valid
        if (StringUtils.isNotBlank(taskInfo.getChildTaskIds())) {
            String[] childTaskIds = taskInfo.getChildTaskIds().split(",");
            for (String childTaskId : childTaskIds) {
                if (StringUtils.isNotBlank(childTaskId) && isNumeric(childTaskId)) {
                    TaskInfo childTaskInfo = taskInfoDao.load(Integer.parseInt(childTaskId));
                    if (Objects.isNull(childTaskInfo)) {
                        return WebResponse.fail("不存在子任务");
                    }
                } else {
                    return WebResponse.fail("不存在子任务");
                }
            }
        }

        return null;
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public WebResponse<String> update(User user, TaskInfo taskInfo) {
        if (!user.isAdmin() && user.getId() != taskInfo.getUserId()) {
            //非admin不得修改别人task
            return WebResponse.fail("用户不匹配");
        }

        WebResponse<String> validWebResponse = validTaskInfo(taskInfo);
        if (Objects.nonNull(validWebResponse)) {
            return validWebResponse;
        }

        TaskInfo exist = taskInfoDao.load(taskInfo.getId());
        if (exist == null) {
            return WebResponse.fail("不存在任务");
        }

        //刷新下次触发时间
        long nextTriggerTime = exist.getTriggerNextTime();
        if (exist.getTriggerStatus() == 1 && !taskInfo.getTimeStr().equals(exist.getTimeStr())) {
            try {
                nextTriggerTime = TimeType.getByName(taskInfo.getTimeType()).parseTime(taskInfo.getTimeStr());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return WebResponse.fail("解析时间错误");
            }
        }

        exist.setJobId(taskInfo.getJobId());
        exist.setTimeType(taskInfo.getTimeType());
        exist.setTimeStr(taskInfo.getTimeStr());
        exist.setDesc(taskInfo.getDesc());
        exist.setUpdateTime(new Date());
        exist.setRouteStrategy(taskInfo.getRouteStrategy());
        exist.setType(taskInfo.getType());
        exist.setParam(taskInfo.getParam());
        exist.setExecStrategy(taskInfo.getExecStrategy());
        exist.setExecTimeout(taskInfo.getExecTimeout());
        exist.setRetryTimes(taskInfo.getRetryTimes());
        exist.setChildTaskIds(taskInfo.getChildTaskIds());
        exist.setTriggerNextTime(nextTriggerTime);
        taskInfoDao.update(exist);

        return WebResponse.success();
    }

    @Override
    public WebResponse<String> remove(int id) {
        TaskInfo task = taskInfoDao.load(id);
        if (task == null) {
            return WebResponse.success();
        }

        taskInfoDao.delete(id);
        //取消task执行
        KinSchedulerContext.instance().getDriver().cancelTask(String.valueOf(task.getId()));
        return WebResponse.success();
    }

    @Override
    public WebResponse<String> start(User user, int id) {
        TaskInfo taskInfo = taskInfoDao.load(id);

        if (!user.isAdmin() && user.getId() != taskInfo.getUserId()) {
            //非admin不得修改别人task
            return WebResponse.fail("用户不匹配");
        }

        if (taskInfo.getTriggerStatus() == 1) {
            return WebResponse.fail("task正在调度中");
        }

        //刷新下次触发时间
        long nextTriggerTime;
        try {
            nextTriggerTime = TimeType.getByName(taskInfo.getTimeType()).parseTime(taskInfo.getTimeStr());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return WebResponse.fail("解析时间错误");
        }

        taskInfo.setTriggerStatus(TaskInfo.START);
        taskInfo.setTriggerLastTime(0);
        taskInfo.setTriggerNextTime(nextTriggerTime);

        taskInfoDao.update(taskInfo);
        return WebResponse.success();
    }

    @Override
    public WebResponse<String> stop(User user, int id) {
        TaskInfo taskInfo = taskInfoDao.load(id);

        if (!user.isAdmin() && user.getId() != taskInfo.getUserId()) {
            //非admin不得修改别人task
            return WebResponse.fail("用户不匹配");
        }

        stop(taskInfo);
        return WebResponse.success();
    }

    private void stop(TaskInfo taskInfo) {
        taskInfo.stop();
        taskInfoDao.update(taskInfo);
    }

    @Override
    public Map<String, Object> dashboardInfo() {
        int taskInfoCount = taskInfoDao.count();
        int taskLogCount = taskLogDao.countByHandleCode(-1);
        int taskLogSuccessCount = taskLogDao.countByHandleCode(TaskLog.SUCCESS);

        Map<String, Object> dashboardMap = new HashMap<>(3);
        dashboardMap.put("jobInfoCount", taskInfoCount);
        dashboardMap.put("jobLogCount", taskLogCount);
        dashboardMap.put("jobLogSuccessCount", taskLogSuccessCount);
        return dashboardMap;
    }

    @Override
    public WebResponse<String> kill(int logId) {
        // base check
        TaskLog taskLog = taskLogDao.load(logId);
        TaskInfo taskInfo = taskInfoDao.load(taskLog.getJobId());
        if (taskInfo == null) {
            return WebResponse.fail("unknown task");
        }
        if (TaskLog.SUCCESS != taskLog.getTriggerCode()) {
            return WebResponse.fail("task trigger fail");
        }

        WebResponse<String> response;
        try {
            KinSchedulerContext.instance().getDriver().cancelTask(String.valueOf(taskLog.getTaskId()));
            taskLog.setHandleCode(TaskLog.FAILURE);
            taskLog.setHandleTime(new Date());
            taskLogDao.updateHandleInfo(taskLog);

            MailUtils.sendAlarmEmail(taskInfo, taskLog, "被取消了");

            response = WebResponse.success();
        } catch (Exception e) {
            log.error("cancel task error >>>", e);
            response = WebResponse.fail(ExceptionUtils.getExceptionDesc(e));
        }
        return response;
    }
}
