package org.kin.scheduler.admin.intercepter;

import org.kin.scheduler.admin.domain.Permission;
import org.kin.scheduler.admin.entity.User;
import org.kin.scheduler.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author huangjianqin
 * @date 2019/7/26
 */
@Component
public class PermissionInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return super.preHandle(request, response, handler);
        }

        boolean needLogin = true;
        boolean needAdmin = false;
        HandlerMethod method = (HandlerMethod) handler;
        Permission permission = method.getMethodAnnotation(Permission.class);
        if (permission != null) {
            needLogin = permission.login();
            needAdmin = permission.admin();
        }

        if (needLogin) {
            User user = userService.getLoginUser(request, response);
            if (user == null) {
                //TODO 跳转到登陆页面
            }
            if (needAdmin && !user.isAdmin()) {
                throw new RuntimeException("权限拦截");
            }
        }

        return super.preHandle(request, response, handler);
    }
}
