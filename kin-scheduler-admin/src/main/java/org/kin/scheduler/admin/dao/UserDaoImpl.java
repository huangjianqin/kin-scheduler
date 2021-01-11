package org.kin.scheduler.admin.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.kin.framework.mybatis.DaoSupport;
import org.kin.scheduler.admin.entity.User;
import org.kin.scheduler.admin.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2021/1/10
 */
@Component
public class UserDaoImpl extends DaoSupport<User, UserMapper> implements UserDao {
    @Override
    public List<User> pageList(int page, int pageSize, String account, int role) {
        LambdaQueryWrapper<User> query =
                Wrappers.lambdaQuery(User.class)
                        .like(User::getAccount, account)
                        .eq(User::getRole, role);
        Page<User> userPage = new Page<>(page, pageSize);
        mapper.selectPage(userPage, query);
        return userPage.getRecords();
    }

    @Override
    public User loadByAccount(String account) {
        return mapper.selectOne(Wrappers.lambdaQuery(User.class).eq(User::getAccount, account));
    }

    @Override
    public int delete(int id) {
        return mapper.deleteById(id);
    }
}
