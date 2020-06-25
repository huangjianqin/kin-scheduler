package org.kin.scheduler.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kin.scheduler.admin.entity.User;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@Mapper
public interface UserDao {
    /**
     * 用户分页
     *
     * @param offset   偏移量, 相当于第n页*每页显示数量
     * @param pageSize 每页显示数量
     * @param account  账号过滤规划
     * @param role     角色
     */
    List<User> pageList(@Param("offset") int offset,
                        @Param("pageSize") int pageSize,
                        @Param("account") String account,
                        @Param("role") int role);

    /**
     * 用户分页(每页)数量
     * @param offset 偏移量, 相当于第n页*每页显示数量
     * @param pageSize 每页显示数量
     * @param account 账号过滤规划
     * @param role 角色
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pageSize") int pageSize,
                      @Param("account") String account,
                      @Param("role") int role);

    /**
     * 加载某个账号信息
     * @param account 账号
     */
    User loadByAccount(@Param("account") String account);

    /**
     * 新建用户
     * @param user 用户信息
     */
    int save(User user);

    /**
     * 更新库
     * @param user 用户信息
     */
    int update(User user);

    /**
     * 删除用户
     * @param id 用户id
     */
    int delete(@Param("id") int id);
}
