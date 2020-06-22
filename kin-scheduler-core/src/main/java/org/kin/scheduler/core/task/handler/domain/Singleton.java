package org.kin.scheduler.core.task.handler.domain;

import java.lang.annotation.*;

/**
 * 标识taskhandler是否允许单例模式执行
 *
 * @author huangjianqin
 * @date 2020-02-21
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Singleton {
}
