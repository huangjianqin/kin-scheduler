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
    /**
     * task分页
     *
     * @param offset        偏移量, 相当于第n页*每页显示数量
     * @param pageSize      每页显示数量
     * @param jobId         job id
     * @param triggerStatus 触发状态规则
     * @param desc          描述规则
     * @param type          任务类型规则
     * @param userName      创建用户名规则
     */
    List<TaskInfoVO> pageList(@Param("offset") int offset,
                              @Param("pageSize") int pageSize,
                              @Param("jobId") int jobId,
                              @Param("triggerStatus") int triggerStatus,
                              @Param("desc") String desc,
                              @Param("type") String type,
                              @Param("userName") String userName);

    /**
     * task分页数量
     * @param offset 偏移量, 相当于第n页*每页显示数量
     * @param pageSize 每页显示数量
     * @param jobId job id
     * @param triggerStatus 触发状态规则
     * @param desc 描述规则
     * @param type 任务类型规则
     * @param userName 创建用户名规则
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pageSize") int pageSize,
                      @Param("jobId") int jobId,
                      @Param("triggerStatus") int triggerStatus,
                      @Param("desc") String desc,
                      @Param("type") String type,
                      @Param("userName") String userName);

    /**
     * 新建task
     * @param taskInfo task信息
     */
    int save(TaskInfo taskInfo);

    /**
     * 加载task
     * @param id task id
     */
    TaskInfo load(@Param("id") int id);

    /**
     * 更新task
     * @param taskInfo task信息
     */
    int update(TaskInfo taskInfo);

    /**
     * 删除task
     * @param id task id
     */
    int delete(@Param("id") int id);

    /**
     * 根据job id获取task
     * @param jobId job id
     */
    List<TaskInfoVO> getTasksByJob(@Param("jobId") int jobId);

    /**
     * 统计task数量
     */
    int count();

    /**
     * 调度task查询, 查询未来近n秒内需要触发的task
     * @param maxNextTime 未来近n秒的触发时间
     */
    List<TaskInfo> scheduleTaskQuery(@Param("maxNextTime") long maxNextTime);

    /**
     * 更新task触发状态
     * @param taskInfo task触发状态信息
     */
    int scheduleUpdate(TaskInfo taskInfo);
}
