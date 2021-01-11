package org.kin.scheduler.admin;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.MySqlDialect;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.kin.framework.utils.JSON;
import org.kin.scheduler.admin.core.TaskScheduleKeeper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@SpringBootApplication(scanBasePackages = {"org.kin.scheduler", "org.kin.framework.web"})
@EnableCaching
@EnableTransactionManagement(proxyTargetClass = true)
public class KinSchedulerApplication {
    public static void main(String[] args) {
        //允许不存在字段
        JSON.PARSER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //允许 不存在双引号key的情况
        JSON.PARSER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        SpringApplication.run(KinSchedulerApplication.class);
        try {
            TaskScheduleKeeper.instance();
            synchronized (KinSchedulerApplication.class) {
                KinSchedulerApplication.class.wait();
            }
        } catch (InterruptedException e) {

        } finally {
            TaskScheduleKeeper.instance().stop();
        }
    }

    /**
     * 添加mybatis plus分页interceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor mpInterceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor interceptor = new PaginationInnerInterceptor();
        interceptor.setDbType(DbType.MYSQL);
        interceptor.setDialect(new MySqlDialect());
        mpInterceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return mpInterceptor;
    }
}
