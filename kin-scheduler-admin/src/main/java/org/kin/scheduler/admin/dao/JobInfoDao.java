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
    List<JobInfo> findAll();

    int save(JobInfo jobInfo);

    int update(JobInfo jobInfo);

    int remove(@Param("id") int id);

    JobInfo load(@Param("id") int id);
}
