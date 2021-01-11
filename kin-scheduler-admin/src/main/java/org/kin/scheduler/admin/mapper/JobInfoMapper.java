package org.kin.scheduler.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.kin.scheduler.admin.entity.JobInfo;

/**
 * @author huangjianqin
 * @date 2021/1/10
 */
@Mapper
public interface JobInfoMapper extends BaseMapper<JobInfo> {
}
