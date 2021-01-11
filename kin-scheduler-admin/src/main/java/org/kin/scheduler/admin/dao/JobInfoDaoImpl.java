package org.kin.scheduler.admin.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.kin.framework.mybatis.DaoSupport;
import org.kin.scheduler.admin.entity.JobInfo;
import org.kin.scheduler.admin.mapper.JobInfoMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2021/1/11
 */
@Component
public class JobInfoDaoImpl extends DaoSupport<JobInfo, JobInfoMapper> implements JobInfoDao {
    @Override
    public List<JobInfo> pageList(int page, int pageSize, String appName, String title) {
        LambdaQueryWrapper<JobInfo> query =
                Wrappers.lambdaQuery(JobInfo.class)
                        .like(JobInfo::getAppName, appName)
                        .like(JobInfo::getTitle, title);
        Page<JobInfo> infoPage = new Page<>(page, pageSize);
        mapper.selectPage(infoPage, query);
        return infoPage.getRecords();
    }

    @Override
    public int remove(int id) {
        return mapper.deleteById(id);
    }

    @Override
    public JobInfo load(int id) {
        return mapper.selectById(id);
    }
}
