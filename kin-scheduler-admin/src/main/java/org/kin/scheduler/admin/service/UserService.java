package org.kin.scheduler.admin.service;

import org.kin.scheduler.admin.domain.WebResponse;
import org.kin.scheduler.admin.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
public interface UserService {
    WebResponse<String> login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean ifRemember);

    WebResponse<String> logout(HttpServletRequest request, HttpServletResponse response);

    User getLoginUser(HttpServletRequest request, HttpServletResponse response);
}
