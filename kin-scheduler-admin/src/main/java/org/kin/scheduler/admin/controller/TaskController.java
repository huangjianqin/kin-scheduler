package org.kin.scheduler.admin.controller;

import org.kin.framework.utils.StringUtils;
import org.kin.framework.web.domain.Permission;
import org.kin.framework.web.domain.WebResponse;
import org.kin.scheduler.admin.core.KinSchedulerContext;
import org.kin.scheduler.admin.core.domain.TaskInfoDTO;
import org.kin.scheduler.admin.dao.TaskInfoDao;
import org.kin.scheduler.admin.domain.TaskType;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.kin.scheduler.admin.entity.User;
import org.kin.scheduler.admin.service.JobService;
import org.kin.scheduler.admin.service.UserService;
import org.kin.scheduler.admin.vo.TaskInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-03-08
 */
@RestController
@RequestMapping("/task")
public class TaskController {
    @Autowired
    private JobService jobService;
    @Autowired
    private TaskInfoDao taskInfoDao;
    @Autowired
    private UserService userService;

    @RequestMapping("/pageList")
    @ResponseBody
    @Permission
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobId, int triggerStatus, String desc, String type, String userName) {
        return jobService.pageList(start, length, jobId, triggerStatus, desc, type, userName);
    }

    @RequestMapping("/add")
    @ResponseBody
    @Permission
    public WebResponse<String> add(HttpServletRequest request, HttpServletResponse response, TaskInfo taskInfo) {
        User user = userService.getLoginUser(request, response);
        taskInfo.setUserId(user.getId());
        return jobService.add(taskInfo);
    }

    @RequestMapping("/update")
    @ResponseBody
    @Permission
    public WebResponse<String> update(HttpServletRequest request, HttpServletResponse response, TaskInfo taskInfo) {
        User user = userService.getLoginUser(request, response);
        return jobService.update(user, taskInfo);
    }

    @RequestMapping("/remove")
    @ResponseBody
    @Permission
    public WebResponse<String> remove(int id) {
        return jobService.remove(id);
    }

    @RequestMapping("/stop")
    @ResponseBody
    @Permission
    public WebResponse<String> pause(HttpServletRequest request, HttpServletResponse response, int id) {
        User user = userService.getLoginUser(request, response);
        return jobService.stop(user, id);
    }

    @RequestMapping("/start")
    @ResponseBody
    @Permission
    public WebResponse<String> start(HttpServletRequest request, HttpServletResponse response, int id) {
        User user = userService.getLoginUser(request, response);
        return jobService.start(user, id);
    }

    @RequestMapping("/trigger")
    @ResponseBody
    @Permission
    public WebResponse<String> triggerJob(int id, String forceParam) {
        TaskInfo taskInfo = taskInfoDao.load(id);
        if (Objects.isNull(taskInfo)) {
            return WebResponse.fail("unknown task");
        }

        TaskType taskType = TaskType.getByName(taskInfo.getType());

        TaskInfoDTO taskInfoDTO = taskInfo.convert();
        if (StringUtils.isNotBlank(forceParam) && taskType.validParam(forceParam)) {
            //强制替换参数
            taskInfoDTO.setParam(forceParam);
        }
        KinSchedulerContext.instance().getDriver().submitTask(taskInfoDTO);

        return WebResponse.success();
    }

    @RequestMapping("/getTasksByJob")
    @ResponseBody
    @Permission
    public WebResponse<List<TaskInfoVO>> getTasksByJob(int jobId) {
        return WebResponse.success(taskInfoDao.getTasksByJob(jobId));
    }

    @RequestMapping("/kill")
    @ResponseBody
    @Permission
    public WebResponse<String> kill(int id) {
        return jobService.kill(id);
    }
}
