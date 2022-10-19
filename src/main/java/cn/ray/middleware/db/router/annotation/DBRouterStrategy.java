package cn.ray.middleware.db.router.annotation;

import java.lang.annotation.*;

/**
 * @author Ray
 * @date 2022/10/19 11:36
 * @description 路由策略,分表标记
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface DBRouterStrategy {

    boolean spiltTable() default false;
}
