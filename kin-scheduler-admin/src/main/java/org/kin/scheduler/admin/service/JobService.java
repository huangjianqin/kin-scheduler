package org.kin.scheduler.admin.service;

import org.kin.scheduler.admin.domain.WebResponse;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.kin.scheduler.admin.entity.User;

import java.util.Map;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
public interface JobService {
    Map<String, Object> pageList(int start, int length, int jobId, int triggerStatus, String desc, String type, String userName);

    WebResponse<String> add(TaskInfo taskInfo);

    WebResponse<String> update(User user, TaskInfo taskInfo);

    WebResponse<String> remove(int id);

    WebResponse<String> start(User user, int id);

    WebResponse<String> stop(User user, int id);

    Map<String, Object> dashboardInfo();

    WebResponse<String> kill(int id);
}
