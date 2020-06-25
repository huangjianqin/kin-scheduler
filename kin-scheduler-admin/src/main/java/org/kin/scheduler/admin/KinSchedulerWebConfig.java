package org.kin.scheduler.admin;

import org.kin.framework.web.interceptor.CookiesInterceptor;
import org.kin.scheduler.admin.interceptor.KinSchedulerPermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * web mvc相关配置
 *
 * @author huangjianqin
 * @date 2020-03-07
 */
@Configuration
public class KinSchedulerWebConfig extends SpringBootServletInitializer implements WebMvcConfigurer {
    /**
     * 权限拦截器
     */
    @Autowired
    private KinSchedulerPermissionInterceptor kinSchedulerPermissionInterceptor;
    /**
     * cookies缓存拦截器
     */
    @Autowired
    private CookiesInterceptor cookiesInterceptor;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        //加载配置类
        return builder.sources(KinSchedulerApplication.class);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cookiesInterceptor).addPathPatterns("/**");
        registry.addInterceptor(kinSchedulerPermissionInterceptor).addPathPatterns("/**");
    }
}
