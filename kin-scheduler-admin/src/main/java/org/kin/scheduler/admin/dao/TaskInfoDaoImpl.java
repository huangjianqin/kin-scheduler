package org.kin.scheduler.admin.dao;

import org.kin.framework.mybatis.DaoSupport;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.kin.scheduler.admin.mapper.TaskInfoMapper;
import org.springframework.stereotype.Component;

/**
 * @author huangjianqin
 * @date 2021/1/11
 */
@Component
public class TaskInfoDaoImpl extends DaoSupport<TaskInfo, TaskInfoMapper> implements TaskInfoDao {

}
