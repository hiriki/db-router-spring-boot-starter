package cn.ray.middleware.db.router;

import cn.ray.middleware.db.router.annotation.DBRouter;
import cn.ray.middleware.db.router.strategy.IDbRouterStrategy;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ray
 * @date 2022/10/18 14:26
 * @description 数据路由切面,通过自定义注解,拦截被切面方法,进行数据路由
 */
@Aspect
public class DbRouterJoinPoint {

    private Logger logger = LoggerFactory.getLogger(DbRouterJoinPoint.class);

    private DbRouterConfig dbRouterConfig;

    private IDbRouterStrategy dbRouterStrategy;

    public DbRouterJoinPoint(DbRouterConfig dbRouterConfig, IDbRouterStrategy dbRouterStrategy) {
        this.dbRouterConfig = dbRouterConfig;
        this.dbRouterStrategy = dbRouterStrategy;
    }

    /**
     *  匹配方法被注解DBRouter标注了的所有方法级的JoinPoint
      */
    @Pointcut("@annotation(cn.ray.middleware.db.router.annotation.DBRouter)")
    public void aopPoint() {
    }

    /**
     * 声明切入的环绕逻辑（即在方法执行前后切入逻辑）
     * aopPoint() && @annotation(dbRouter) 表示逻辑必须确保被dbRouter标注，并传入DBRouter
     * @param jp
     * @param dbRouter
     * @return
     */
    @Around("aopPoint() && @annotation(dbRouter)")
    public Object dbRouter(ProceedingJoinPoint jp, DBRouter dbRouter) {
        // 获取注解中的分库分表字段
        String dbKey = dbRouter.key();
        // 如果为空,则默认应用配置文件中的routerKey
        dbKey = StringUtils.isNotBlank(dbKey) ? dbKey : dbRouterConfig.getRouterKey();
        if (StringUtils.isBlank(dbRouter.key()) && StringUtils.isBlank(dbRouterConfig.getRouterKey())) {
            throw new RuntimeException("annotation DBRouter key is null!");
        }

        // 获取路由字段
        String dbKeyAttr = getAttrValue(dbKey, jp.getArgs());

        // 路由策略
        dbRouterStrategy.dbRouter(dbKeyAttr);

        // 放行，返回结果
        try {
            return jp.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            // 每次路由之后需清空
            dbRouterStrategy.clear();
        }
    }

    private String getAttrValue(String attr,Object[] args) {
        if (1 == args.length) {
            Object arg = args[0];
            if (arg instanceof String) {
                return arg.toString();
            }
        }

        String fieldValue = null;
        for (Object arg : args) {
            if (StringUtils.isNotBlank(fieldValue)) {
                break;
            }
            try {
                // 指定Bean的指定属性值都作为 String 返回
                // 这里即 找到 与 路由字段 同名的 入参 并拿到其 String 值
                fieldValue = BeanUtils.getProperty(arg,attr);
            } catch (Exception e) {
                logger.error("获取路由属性值失败,attr: {}",attr);
            }
        }
        return fieldValue;
    }
}
