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
     * 查找所有job
     */
    List<JobInfo> findAll();

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
