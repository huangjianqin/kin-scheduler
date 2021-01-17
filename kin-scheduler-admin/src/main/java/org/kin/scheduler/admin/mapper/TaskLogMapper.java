package org.kin.scheduler.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kin.scheduler.admin.entity.TaskLog;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2021/1/10
 */
@Mapper
public interface TaskLogMapper extends BaseMapper<TaskLog> {
    /**
     * task log 分页
     *
     * @param offset           偏移量, 相当于第n页*每页显示数量
     * @param pageSize         每页显示数量
     * @param userId           操作用户id规则
     * @param userRole         操作用户 角色规则
     * @param jobId            job id规则
     * @param taskId           task id规则
     * @param triggerTimeStart 触发时间规则
     * @param triggerTimeEnd   触发结束时间规则
     * @param logStatus        log状态, 1:已触发, 2:未触发或者未成功执行, 3:已触发且成功执行
     */
    List<TaskLog> pageList(@Param("offset") int offset,
                           @Param("pageSize") int pageSize,
                           @Param("userId") int userId,
                           @Param("userRole") int userRole,
                           @Param("jobId") int jobId,
                           @Param("taskId") int taskId,
                           @Param("triggerTimeStart") Date triggerTimeStart,
                           @Param("triggerTimeEnd") Date triggerTimeEnd,
                           @Param("logStatus") int logStatus);

    /**
     * 更新触发信息
     *
     * @param taskLog task log信息
     */
    int updateTriggerInfo(TaskLog taskLog);

    /**
     * 更新task执行完信息
     *
     * @param taskLog tasklog 信息
     * @return
     */
    int updateHandleInfo(TaskLog taskLog);

    /**
     * 删除某task的log
     *
     * @param taskId task id
     * @return
     */
    int deleteByTaskId(@Param("taskId") int taskId);

    /**
     * 统计某@param handleCode task log数
     *
     * @param handleCode task执行结果状态
     */
    int countByHandleCode(@Param("handleCode") int handleCode);

    /**
     * 根据时间区间统计已触发(触发就行, 不管执行成功与否)或者已执行成功的task log信息
     *
     * @param from 开始时间
     * @param to   结束时间
     */
    List<Map<String, Object>> countByDay(@Param("from") Date from,
                                         @Param("to") Date to);

    /**
     * 清掉task log
     *
     * @param jobId           job id规则
     * @param taskId          task id规则
     * @param clearBeforeTime 清除XX时间前
     * @param clearBeforeNum  清除数量
     * @return
     */
    int clearLog(@Param("jobId") int jobId,
                 @Param("taskId") int taskId,
                 @Param("clearBeforeTime") Date clearBeforeTime,
                 @Param("clearBeforeNum") int clearBeforeNum);

    /**
     * 获取执行失败task id(可能成功触发了)
     */
    List<Integer> findFailTaskLogIds();
}
