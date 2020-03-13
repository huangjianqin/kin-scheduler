package org.kin.scheduler.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.kin.scheduler.admin.vo.TaskInfoVO;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@Mapper
public interface TaskInfoDao {
    List<TaskInfoVO> pageList(@Param("offset") int offset,
                              @Param("pageSize") int pageSize,
                              @Param("jobId") int jobId,
                              @Param("triggerStatus") int triggerStatus,
                              @Param("desc") String desc,
                              @Param("type") String type,
                              @Param("userName") String userName);

    int pageListCount(@Param("offset") int offset,
                      @Param("pageSize") int pageSize,
                      @Param("jobId") int jobId,
                      @Param("triggerStatus") int triggerStatus,
                      @Param("desc") String desc,
                      @Param("type") String type,
                      @Param("userName") String userName);

    int save(TaskInfo taskInfo);

    TaskInfo load(@Param("id") int id);

    int update(TaskInfo taskInfo);

    int delete(@Param("id") int id);

    List<TaskInfoVO> getTasksByJob(@Param("jobId") int jobId);

    int count();

    List<TaskInfo> scheduleTaskQuery(@Param("maxNextTime") long maxNextTime);

    int scheduleUpdate(TaskInfo taskInfo);
}
