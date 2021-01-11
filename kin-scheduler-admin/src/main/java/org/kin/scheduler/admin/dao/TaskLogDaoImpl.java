package org.kin.scheduler.admin.dao;

import org.kin.framework.mybatis.DaoSupport;
import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.admin.mapper.TaskLogMapper;
import org.springframework.stereotype.Component;

/**
 * @author huangjianqin
 * @date 2021/1/12
 */
@Component
public class TaskLogDaoImpl extends DaoSupport<TaskLog, TaskLogMapper> implements TaskLogDao {
}
