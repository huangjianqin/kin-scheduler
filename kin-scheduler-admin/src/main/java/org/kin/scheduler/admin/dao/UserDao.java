package org.kin.scheduler.admin.dao;

import org.kin.framework.mybatis.BaseDao;
import org.kin.scheduler.admin.entity.User;
import org.kin.scheduler.admin.mapper.UserMapper;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
public interface UserDao extends BaseDao<User, UserMapper> {
    /**
     * 用户分页
     *
     * @param page     第n页
     * @param pageSize 每页显示数量
     * @param account  账号过滤规划
     * @param role     角色
     */
    List<User> pageList(int page,
                        int pageSize,
                        String account,
                        int role);

    /**
     * 加载某个账号信息
     *
     * @param account 账号
     */
    User loadByAccount(String account);

    /**
     * 删除用户
     *
     * @param id 用户id
     */
    int delete(int id);
}
