package org.kin.scheduler.admin.interceptor;

import org.kin.framework.web.interceptor.PermissionInterceptor;
import org.kin.scheduler.admin.entity.User;
import org.kin.scheduler.admin.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-06-26
 */
@Component
public class KinSchedulerPermissionInterceptor extends PermissionInterceptor {
    private static final Logger log = LoggerFactory.getLogger("permission");

    @Autowired
    private UserService userService;

    @Override
    public boolean customCheckLogin(HttpServletRequest request, HttpServletResponse response, boolean needAdmin) {
        User user = userService.getLoginUser(request, response);
        if (Objects.isNull(user)) {
            //TODO 跳转到登陆页面
            return false;
        }
        if (needAdmin && !user.isAdmin()) {
            throw new IllegalStateException(String.format("用户%s(%s)没有管理员权限", user.getAccount(), user.getId()));
        }

        return true;
    }
}
