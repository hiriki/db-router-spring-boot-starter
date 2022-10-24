package cn.ray.middleware.db.router.dynamic;

import cn.ray.middleware.db.router.DbContextHolder;
import cn.ray.middleware.db.router.annotation.DBRouterStrategy;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ray
 * @date 2022/10/19 11:14
 * @description MyBatis拦截器，对SQL语句进行拦截，处理分表信息
 */
@Intercepts({@Signature(type = StatementHandler.class,method = "prepare",args = {Connection.class, Integer.class})})
public class DynamicMyBatisPlugin implements Interceptor {

    private Pattern pattern = Pattern.compile("(from|into|update)[\\s]{1,}(\\w{1,})", Pattern.CASE_INSENSITIVE);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取MetaObject
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        // MetaObject 是 Mybatis 反射工具类，通过 MetaObject 获取和设置对象的属性值
        MetaObject metaObject = MetaObject.forObject(statementHandler, SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());
        // 每个MappedStatement对应了我们自定义Mapper接口中的一个方法，它保存了开发人员编写的SQL语句、参数结构、返回值结构、Mybatis对它的处理方式的配置等细节要素，是对一个SQL命令是什么、执行方式的完整定义。可以说，有了它Mybatis就知道如何去调度四大组件顺利的完成用户请求。
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        /*
          当前MappedStatement的唯一识别ID，并且在同一个Configuration中是唯一的
          它由Mapper类的完全限定名和Mapper方法名称拼接而成
         */
        String id = mappedStatement.getId();
        // 获取Mapper类
        String className = id.substring(0, id.lastIndexOf("."));
        Class<?> mapper = Class.forName(className);

        // 获取自定义注解判断是否分表
        DBRouterStrategy dbRouterStrategy = mapper.getAnnotation(DBRouterStrategy.class);
        if ( dbRouterStrategy == null || !dbRouterStrategy.spiltTable()) {
            return invocation.proceed();
        }

        // 获取SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql();

        Matcher matcher = pattern.matcher(sql);

        // 匹配获取表名
        String tableName = null;
        // find()是部分匹配，从当前位置开始匹配，找到一个匹配的子串，将移动下次匹配的位置。
        if (matcher.find()) {
            tableName = matcher.group().trim();
        }

        // 替换SQL
        assert tableName != null;
        String replaceSql = matcher.replaceAll(tableName + "_" + DbContextHolder.getTBKey());

        // 通过反射修改SQL语句
        // 获取 sql 字段
        Field field = boundSql.getClass().getDeclaredField("sql");
        // 由于JDK的安全检查耗时较多.所以通过setAccessible(true)的方式关闭安全检查就可以达到提升反射速度的目的
        field.setAccessible(true);
        // set 将指定对象参数上此 Field 对象表示的字段的值设置为作为参数传递的指定新值
        field.set(boundSql,replaceSql);
        // 修改后需重新开启安全检查
        field.setAccessible(false);

        // 放行
        return invocation.proceed();
    }
}
