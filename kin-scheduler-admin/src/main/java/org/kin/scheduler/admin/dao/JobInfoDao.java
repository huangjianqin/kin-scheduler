package org.kin.scheduler.admin.dao;

import org.kin.framework.mybatis.BaseDao;
import org.kin.scheduler.admin.entity.JobInfo;
import org.kin.scheduler.admin.mapper.JobInfoMapper;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
public interface JobInfoDao extends BaseDao<JobInfo, JobInfoMapper> {
    /**
     * task分页
     *
     * @param page     偏移量, 相当于第n页*每页显示数量
     * @param pageSize 每页显示数量
     * @param appName  appName规则
     * @param title    title规则
     */
    List<JobInfo> pageList(int page,
                           int pageSize,
                           String appName,
                           String title);

    /**
     * 删除
     * @param id job id
     */
    int remove(int id);

    /**
     * 加载job
     * @param id job id
     */
    JobInfo load(int id);
}
