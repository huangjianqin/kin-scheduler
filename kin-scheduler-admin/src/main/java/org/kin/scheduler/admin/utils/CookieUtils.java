package org.kin.scheduler.admin.utils;

import org.kin.framework.web.utils.Cookies;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-06-26
 */
public class CookieUtils extends Cookies {
    // 默认缓存时间,单位/秒
    private static final int COOKIE_MAX_AGE = (int) TimeUnit.HOURS.toSeconds(2);
    // 保存路径,根路径
    public static final String COOKIE_PATH = "/";

    public static final String LOGIN_IDENTITY = "IDENTITY";

    public static void set(HttpServletResponse response, String key, String value, boolean ifRemember) {
        int age = ifRemember ? COOKIE_MAX_AGE : -1;
        set(response, key, value, null, COOKIE_PATH, age, true);
    }

    public static void remove(HttpServletRequest request, HttpServletResponse response, String key) {
        remove(request, response, key, COOKIE_PATH);
    }
}
