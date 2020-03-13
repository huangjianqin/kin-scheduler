package org.kin.scheduler.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kin.scheduler.admin.entity.TaskLog;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@Mapper
public interface TaskLogDao {
    List<TaskLog> pageList(@Param("offset") int offset,
                           @Param("pageSize") int pageSize,
                           @Param("userId") int userId,
                           @Param("userRole") int userRole,
                           @Param("jobId") int jobId,
                           @Param("taskId") int taskId,
                           @Param("triggerTimeStart") Date triggerTimeStart,
                           @Param("triggerTimeEnd") Date triggerTimeEnd,
                           @Param("logStatus") int logStatus);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("userId") int userId,
                      @Param("userRole") int userRole,
                      @Param("jobId") int jobId,
                      @Param("taskId") int taskId,
                      @Param("triggerTimeStart") Date triggerTimeStart,
                      @Param("triggerTimeEnd") Date triggerTimeEnd,
                      @Param("logStatus") int logStatus);

    TaskLog load(@Param("id") int id);

    int save(TaskLog taskLog);

    int updateTriggerInfo(TaskLog taskLog);

    int updateHandleInfo(TaskLog taskLog);

    int delete(@Param("taskId") int taskId);

    int countByHandleCode(@Param("handleCode") int handleCode);

    List<Map<String, Object>> countByDay(@Param("from") Date from,
                                         @Param("to") Date to);

    int clearLog(@Param("jobId") int jobId,
                 @Param("taskId") int taskId,
                 @Param("clearBeforeTime") Date clearBeforeTime,
                 @Param("clearBeforeNum") int clearBeforeNum);

    List<Integer> findFailJobLogIds();
}
