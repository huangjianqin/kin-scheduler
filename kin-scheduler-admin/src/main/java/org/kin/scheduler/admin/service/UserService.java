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
    /**
     * 登录
     *
     * @param request    请求
     * @param response   返回
     * @param account    账号
     * @param password   密码
     * @param ifRemember 是否保存登录状态
     */
    WebResponse<String> login(HttpServletRequest request, HttpServletResponse response, String account, String password, boolean ifRemember);

    /**
     * 登出
     *
     * @param request  请求
     * @param response 返回
     */
    WebResponse<String> logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * 获取该session登录中的用户
     * @param request 请求
     * @param response 返回
     */
    User getLoginUser(HttpServletRequest request, HttpServletResponse response);

    /**
     * 创建用户
     *
     * @param account  账号
     * @param password 密码
     * @param role     角色
     * @param name     用户名
     */
    WebResponse<String> create(String account, String password, int role, String name);
}
