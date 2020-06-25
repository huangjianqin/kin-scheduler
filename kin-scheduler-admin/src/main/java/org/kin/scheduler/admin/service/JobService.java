package org.kin.scheduler.admin.service;

import org.kin.framework.web.domain.WebResponse;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.kin.scheduler.admin.entity.User;

import java.util.Map;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
public interface JobService {
    /**
     * 获取task列表
     *
     * @param start         开始页
     * @param length        每页task显示数量
     * @param jobId         job id
     * @param triggerStatus 触发状态
     * @param desc          描述
     * @param type          task类型
     * @param userName      用户名
     */
    Map<String, Object> pageList(int start, int length, int jobId, int triggerStatus, String desc, String type, String userName);

    /**
     * 新建task
     * @param taskInfo task描述
     */
    WebResponse<String> add(TaskInfo taskInfo);

    /**
     * 更新task内容
     * @param user 用户
     * @param taskInfo task信息
     */
    WebResponse<String> update(User user, TaskInfo taskInfo);

    /**
     * 移除task
     * @param id task id
     */
    WebResponse<String> remove(int id);

    /**
     * task开始调度
     * @param user 用户
     * @param id task id
     */
    WebResponse<String> start(User user, int id);

    /**
     * task停止调度
     * @param user 用户
     * @param id task id
     */
    WebResponse<String> stop(User user, int id);

    /**
     * task总体信息监控面板
     */
    Map<String, Object> dashboardInfo();

    /**
     * 杀死(取消)task
     * @param id task id
     */
    WebResponse<String> kill(int id);
}
