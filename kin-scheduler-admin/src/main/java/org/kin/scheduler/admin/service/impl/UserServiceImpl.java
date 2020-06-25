package org.kin.scheduler.admin.service.impl;

import org.kin.framework.utils.JSON;
import org.kin.framework.utils.StringUtils;
import org.kin.scheduler.admin.dao.UserDao;
import org.kin.scheduler.admin.domain.CookieKeies;
import org.kin.scheduler.admin.domain.WebResponse;
import org.kin.scheduler.admin.entity.User;
import org.kin.scheduler.admin.service.UserService;
import org.kin.scheduler.admin.utils.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserDao userDao;

    private String makeToken(User user) {
        String tokenJson = JSON.write(user);
        return new BigInteger(tokenJson.getBytes()).toString(16);
    }

    private User parseToken(String tokenHex) {
        User user = null;
        if (tokenHex != null) {
            String tokenJson = new String(new BigInteger(tokenHex, 16).toByteArray());
            user = JSON.read(tokenJson, User.class);
        }
        return user;
    }

    @Override
    public WebResponse<String> login(HttpServletRequest request, HttpServletResponse response, String account, String password, boolean ifRemember) {
        // param
        if (StringUtils.isBlank(account) || StringUtils.isBlank(password)) {
            return WebResponse.fail("参数缺失");
        }

        // valid passowrd
        User user = userDao.loadByAccount(account);
        if (user == null) {
            return WebResponse.fail("不存在用户");
        }

        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!passwordMd5.equals(user.getPassword())) {
            return WebResponse.fail("密码错误");
        }

        String loginToken = makeToken(user);

        // do login
        CookieUtils.set(response, CookieKeies.LOGIN_IDENTITY, loginToken, ifRemember);
        return WebResponse.success("登陆成功", null);
    }

    @Override
    public WebResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.remove(request, response, CookieKeies.LOGIN_IDENTITY);
        return WebResponse.success("登出成功", null);
    }

    @Override
    public User getLoginUser(HttpServletRequest request, HttpServletResponse response) {
        String cookieToken = CookieUtils.getValue(request, CookieKeies.LOGIN_IDENTITY);
        if (cookieToken != null) {
            User cookieUser;
            try {
                cookieUser = parseToken(cookieToken);
            } catch (Exception e) {
                logout(request, response);
                return null;
            }
            if (cookieUser != null) {
                User dbUser = userDao.loadByAccount(cookieUser.getAccount());
                if (dbUser != null) {
                    if (cookieUser.getPassword().equals(dbUser.getPassword())) {
                        return dbUser;
                    }
                }
            }
        }
        return null;
    }
}
