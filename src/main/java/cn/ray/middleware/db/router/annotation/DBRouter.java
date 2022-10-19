package cn.ray.middleware.db.router.annotation;

import java.lang.annotation.*;

/**
 * @author Ray
 * @date 2022/10/14 15:19
 * @description 路由注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface DBRouter {

    /** 分库分表字段 */
    String key() default "";

}
