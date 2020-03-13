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
    List<User> pageList(@Param("offset") int offset,
                        @Param("pageSize") int pageSize,
                        @Param("account") String account,
                        @Param("role") int role);

    int pageListCount(@Param("offset") int offset,
                      @Param("pageSize") int pageSize,
                      @Param("account") String account,
                      @Param("role") int role);

    User loadByAccount(@Param("account") String account);

    int save(User user);

    int update(User user);

    int delete(@Param("id") int id);
}
