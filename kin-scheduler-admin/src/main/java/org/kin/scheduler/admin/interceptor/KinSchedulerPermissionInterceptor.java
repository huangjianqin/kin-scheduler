package org.kin.scheduler.admin.interceptor;

import org.kin.framework.web.interceptor.PermissionInterceptor;
import org.kin.scheduler.admin.entity.User;
import org.kin.scheduler.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author huangjianqin
 * @date 2020-06-26
 */
@Component
public class KinSchedulerPermissionInterceptor extends PermissionInterceptor {
    @Autowired
    private UserService userService;

    @Override
    public void customCheckLogin(HttpServletRequest request, HttpServletResponse response, boolean needAdmin) {
        User user = userService.getLoginUser(request, response);
        if (user == null) {
            //TODO 跳转到登陆页面
        }
        if (needAdmin && !user.isAdmin()) {
            throw new RuntimeException("权限拦截");
        }
    }
}
