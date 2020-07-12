package org.kin.scheduler.admin.controller;

import org.kin.framework.web.domain.Permission;
import org.kin.framework.web.domain.WebResponse;
import org.kin.scheduler.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public WebResponse<String> login(
            HttpServletRequest request,
            HttpServletResponse response,
            String account, String password,
            @RequestParam(defaultValue = "false") String ifRemember) {
        boolean ifRem = ifRemember != null && ifRemember.trim().length() > 0 && "on".equals(ifRemember);
        return userService.login(request, response, account, password, ifRem);
    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    @Permission()
    public WebResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        return userService.logout(request, response);
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public WebResponse<String> create(String account, String password, @RequestParam(defaultValue = "0") int role, String name) {
        return userService.create(account, password, role, name);
    }
}
