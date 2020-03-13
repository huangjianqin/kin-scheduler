package org.kin.scheduler.admin.controller;

import org.kin.scheduler.admin.domain.Permission;
import org.kin.scheduler.admin.domain.WebResponse;
import org.kin.scheduler.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    @Permission(login = false)
    public WebResponse<String> login(HttpServletRequest request, HttpServletResponse response, String account, String password, String ifRemember) {
        boolean ifRem = ifRemember != null && ifRemember.trim().length() > 0 && "on".equals(ifRemember);
        return userService.login(request, response, account, password, ifRem);
    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    @Permission()
    public WebResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        return userService.logout(request, response);
    }

}
