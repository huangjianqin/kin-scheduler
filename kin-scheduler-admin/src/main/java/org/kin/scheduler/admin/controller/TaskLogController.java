package org.kin.scheduler.admin.controller;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.scheduler.admin.core.KinSchedulerContext;
import org.kin.scheduler.admin.dao.TaskLogDao;
import org.kin.scheduler.admin.domain.Permission;
import org.kin.scheduler.admin.domain.WebResponse;
import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.admin.entity.User;
import org.kin.scheduler.admin.service.UserService;
import org.kin.scheduler.core.worker.transport.TaskExecFileContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author huangjianqin
 * @date 2020-03-08
 */
@RestController
@RequestMapping("/taskLog")
public class TaskLogController {
    private static final Logger log = LoggerFactory.getLogger(TaskLogController.class);

    @Autowired
    private TaskLogDao taskLogDao;
    @Autowired
    private UserService userService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new CustomDateEditor(TimeUtils.getDatetimeFormat(), true));
    }

    @RequestMapping("/chartInfo")
    @ResponseBody
    public WebResponse<Map<String, Object>> chartInfo(Date startDate, Date endDate) {
        List<String> dayList = new ArrayList<>();
        List<Integer> runningList = new ArrayList<>();
        List<Integer> sucList = new ArrayList<>();
        List<Integer> failList = new ArrayList<>();
        int runningTotal = 0;
        int sucTotal = 0;
        int failTotal = 0;

        List<Map<String, Object>> triggerCountMap = taskLogDao.countByDay(startDate, endDate);
        if (CollectionUtils.isNonEmpty(triggerCountMap)) {
            for (Map<String, Object> item : triggerCountMap) {
                String day = String.valueOf(item.get("day"));
                int dayCount = Integer.parseInt(String.valueOf(item.get("dayCount")));
                int runningCount = Integer.parseInt(String.valueOf(item.get("runningCount")));
                int sucCount = Integer.parseInt(String.valueOf(item.get("sucCount")));
                int failCount = dayCount - runningCount - sucCount;

                dayList.add(day);
                runningList.add(runningCount);
                sucList.add(sucCount);
                failList.add(failCount);

                runningTotal += runningCount;
                sucTotal += sucCount;
                failTotal += failCount;
            }
        } else {
            for (int i = 4; i > -1; i--) {
                dayList.add(TimeUtils.formatDate(TimeUtils.addDays(new Date(), -i)));
                runningList.add(0);
                sucList.add(0);
                failList.add(0);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("dayList", dayList);
        result.put("runningList", runningList);
        result.put("sucList", sucList);
        result.put("failList", failList);

        result.put("runningTotal", runningTotal);
        result.put("sucTotal", sucTotal);
        result.put("failTotal", failTotal);

        return WebResponse.success(result);
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @Permission
    public Map<String, Object> pageList(HttpServletRequest request, HttpServletResponse response,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobId, int taskId, int logStatus, String filterTime) {
        User user = userService.getLoginUser(request, response);

        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (filterTime != null && filterTime.trim().length() > 0) {
            String[] temp = filterTime.split(" - ");
            if (temp != null && temp.length == 2) {
                triggerTimeStart = TimeUtils.parseDateTime(temp[0]);
                triggerTimeEnd = TimeUtils.parseDateTime(temp[1]);
            }
        }

        // page query
        List<TaskLog> list = taskLogDao.pageList(start, length, user.getId(), user.getRole(), jobId, taskId, triggerTimeStart, triggerTimeEnd, logStatus);
        int list_count = taskLogDao.pageListCount(start, length, user.getId(), user.getRole(), jobId, taskId, triggerTimeStart, triggerTimeEnd, logStatus);

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        // 总记录数
        maps.put("recordsTotal", list_count);
        // 过滤后的总记录数
        maps.put("recordsFiltered", list_count);
        // 分页列表
        maps.put("data", list);
        return maps;
    }

    @RequestMapping("/logDetailCat")
    @ResponseBody
    @Permission
    public WebResponse<TaskExecFileContent> logDetailCat(int logId, int fromLineNum) {
        try {
            TaskExecFileContent taskExecFileContent = KinSchedulerContext.instance().getDriver().readLog(logId, fromLineNum);

            if (Objects.nonNull(taskExecFileContent) && StringUtils.isNotBlank(taskExecFileContent.getContent()) && !taskExecFileContent.isEnd()) {
                TaskLog taskLog = taskLogDao.load(logId);
                if (taskLog.getHandleCode() > 0) {
                    taskExecFileContent.setEnd(true);
                }
                return WebResponse.success(taskExecFileContent);
            } else {
                return WebResponse.fail("remote read log, get null");
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return WebResponse.fail(String.format("remote read log error >>>> %s", ExceptionUtils.getExceptionDesc(e)));
        }
    }

    @RequestMapping("/clearLog")
    @ResponseBody
    @Permission
    public WebResponse<String> clearLog(int jobId, int taskId, int type) {

        Date clearBeforeTime = null;
        int clearBeforeNum = 0;
        if (type == 1) {
            // 清理一个月之前日志数据
            clearBeforeTime = TimeUtils.addMonths(new Date(), -1);
        } else if (type == 2) {
            // 清理三个月之前日志数据
            clearBeforeTime = TimeUtils.addMonths(new Date(), -3);
        } else if (type == 3) {
            // 清理六个月之前日志数据
            clearBeforeTime = TimeUtils.addMonths(new Date(), -6);
        } else if (type == 4) {
            // 清理一年之前日志数据
            clearBeforeTime = TimeUtils.addYears(new Date(), -1);
        } else if (type == 5) {
            // 清理一千条以前日志数据
            clearBeforeNum = 1000;
        } else if (type == 6) {
            // 清理一万条以前日志数据
            clearBeforeNum = 10000;
        } else if (type == 7) {
            // 清理三万条以前日志数据
            clearBeforeNum = 30000;
        } else if (type == 8) {
            // 清理十万条以前日志数据
            clearBeforeNum = 100000;
        } else if (type == 9) {
            // 清理所有日志数据
            clearBeforeNum = 0;
        } else {
            return WebResponse.fail("未知请求类型");
        }

        taskLogDao.clearLog(jobId, taskId, clearBeforeTime, clearBeforeNum);
        return WebResponse.success();
    }
}
