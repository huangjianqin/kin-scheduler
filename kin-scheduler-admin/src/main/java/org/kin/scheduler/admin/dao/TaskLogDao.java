package org.kin.scheduler.admin.dao;

import org.kin.framework.mybatis.BaseDao;
import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.admin.mapper.TaskLogMapper;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
public interface TaskLogDao extends BaseDao<TaskLog, TaskLogMapper> {
}
