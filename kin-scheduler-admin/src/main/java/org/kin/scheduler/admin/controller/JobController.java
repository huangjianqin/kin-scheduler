package org.kin.scheduler.admin.controller;

import org.kin.framework.utils.StringUtils;
import org.kin.scheduler.admin.dao.JobInfoDao;
import org.kin.scheduler.admin.domain.Permission;
import org.kin.scheduler.admin.domain.WebResponse;
import org.kin.scheduler.admin.entity.JobInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@RestController
@RequestMapping("/job")
public class JobController {
    @Autowired
    private JobInfoDao jobInfoDao;

    private WebResponse<String> checkJobInfo(JobInfo jobInfo) {
        // valid
        if (StringUtils.isBlank(jobInfo.getAppName())) {
            return WebResponse.fail("请输入appName");
        }

        int appNameLen = jobInfo.getAppName().length();
        if (appNameLen < 4 || appNameLen > 64) {
            return WebResponse.fail("appName格式出错");
        }
        if (StringUtils.isBlank(jobInfo.getTitle())) {
            return WebResponse.fail("title不能为null");
        }

        return null;
    }

    @RequestMapping("/save")
    @ResponseBody
    @Permission
    public WebResponse<String> save(JobInfo jobInfo) {
        WebResponse<String> checkResp = checkJobInfo(jobInfo);
        if (Objects.nonNull(checkResp)) {
            return checkResp;
        }
        int ret = jobInfoDao.save(jobInfo);
        return (ret > 0) ? WebResponse.success() : WebResponse.fail("db error");
    }

    @RequestMapping("/update")
    @ResponseBody
    @Permission
    public WebResponse<String> update(JobInfo jobInfo) {
        WebResponse<String> checkResp = checkJobInfo(jobInfo);
        if (Objects.nonNull(checkResp)) {
            return checkResp;
        }
        int ret = jobInfoDao.update(jobInfo);
        return (ret > 0) ? WebResponse.success() : WebResponse.fail("db error");
    }

    @RequestMapping("/remove")
    @ResponseBody
    @Permission
    public WebResponse<String> remove(int id) {

        // valid
        JobInfo jobInfo = jobInfoDao.load(id);
        if (Objects.isNull(jobInfo)) {
            return WebResponse.fail("不存在job");
        }

        int ret = jobInfoDao.remove(id);
        return (ret > 0) ? WebResponse.success() : WebResponse.fail("db error");
    }
}
