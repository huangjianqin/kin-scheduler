package org.kin.scheduler.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kin.scheduler.admin.entity.JobInfo;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@Mapper
public interface JobInfoDao {
    /**
     * task分页
     *
     * @param offset        偏移量, 相当于第n页*每页显示数量
     * @param pageSize      每页显示数量
     * @param appName   appName规则
     * @param title title规则
     */
    List<JobInfo> pageList(@Param("offset") int offset,
                           @Param("pageSize") int pageSize,
                           @Param("appName") String appName,
                           @Param("title") String title);

    /**
     * 存库
     *
     * @param jobInfo job信息
     */
    int save(JobInfo jobInfo);

    /**
     * 更新库
     * @param jobInfo job信息
     */
    int update(JobInfo jobInfo);

    /**
     * 删除
     * @param id job id
     */
    int remove(@Param("id") int id);

    /**
     * 加载job
     * @param id job id
     */
    JobInfo load(@Param("id") int id);
}
