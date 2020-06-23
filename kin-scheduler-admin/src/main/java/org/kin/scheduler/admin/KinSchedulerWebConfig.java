package org.kin.scheduler.admin;

import org.kin.scheduler.admin.intercepter.CookiesInterceptor;
import org.kin.scheduler.admin.intercepter.PermissionInterceptor;
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
    @Autowired
    private PermissionInterceptor permissionInterceptor;
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
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/**");
    }
}
