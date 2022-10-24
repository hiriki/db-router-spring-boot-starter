package cn.ray.middleware.db.router.config;

import cn.ray.middleware.db.router.DbRouterConfig;
import cn.ray.middleware.db.router.DbRouterJoinPoint;
import cn.ray.middleware.db.router.dynamic.DynamicDataSource;
import cn.ray.middleware.db.router.dynamic.DynamicMyBatisPlugin;
import cn.ray.middleware.db.router.strategy.IDbRouterStrategy;
import cn.ray.middleware.db.router.strategy.impl.DbRouterStrategyHashCode;
import cn.ray.middleware.db.router.utils.PropertyUtil;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ray
 * @date 2022/10/14 15:25
 * @description
 */
@Configuration
public class DataSourceAutoConfig implements EnvironmentAware {

    /**
     * 数据源配置组
     */
    private Map<String, Map<String, Object>> dataSourceMap = new HashMap<>();

    /**
     * 默认数据源配置
     */
    private Map<String, Object> defaultDataSourceConfig;

    /**
     * 分库数量
     */
    private int dbCount;

    /**
     * 分表数量
     */
    private int tbCount;

    /**
     * 路由字段
     */
    private String routerKey;

    public DataSourceAutoConfig() {
    }

    public DataSourceAutoConfig(int dbCount, int tbCount, String routerKey) {
        this.dbCount = dbCount;
        this.tbCount = tbCount;
        this.routerKey = routerKey;
    }

    @Override
    public void setEnvironment(Environment environment) {
        String prefix = "ray-db-router.jdbc.datasource.";

        // 获取分库分表数量
        dbCount = Integer.valueOf(environment.getProperty(prefix+"dbCount"));
        tbCount = Integer.valueOf(environment.getProperty(prefix+"tbCount"));

        // 获取分库分表字段
        routerKey = environment.getProperty(prefix + "routerKey");

        // 分库分表数据源
        String dataSources = environment.getProperty(prefix + "list");
        assert dataSources != null;
        for (String dbInfo : dataSources.split(",")) {
            // db01、db02
            // prefix + dbInfo = ray-db-router.jdbc.datasource.db01
            Map<String,Object> dataSourceProps = PropertyUtil.handle(environment,prefix + dbInfo,Map.class);
            dataSourceMap.put(dbInfo,dataSourceProps);
        }

        // 默认数据源 ray-db-router.jdbc.datasource.default: db00
        String defaultData = environment.getProperty(prefix + "default");
        defaultDataSourceConfig = PropertyUtil.handle(environment,prefix + defaultData,Map.class);

    }

    @Bean
    public DataSource dataSource() {
        // 创建数据源
        Map<Object,Object> targetDataSources = new HashMap<>();
        for (String dbInfo : dataSourceMap.keySet()) {
            Map<String,Object> objMap = dataSourceMap.get(dbInfo);
            targetDataSources.put(dbInfo,
                    new DriverManagerDataSource(objMap.get("url").toString(),
                            objMap.get("username").toString(),objMap.get("password").toString()));
        }

        // 设置数据源
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(new DriverManagerDataSource(
                defaultDataSourceConfig.get("url").toString(),
                defaultDataSourceConfig.get("username").toString(),
                defaultDataSourceConfig.get("password").toString()
        ));
        return dynamicDataSource;
    }

    @Bean
    public DbRouterConfig dbRouterConfig() {
        return new DbRouterConfig(dbCount, tbCount, routerKey);
    }

    @Bean
    public IDbRouterStrategy dbRouterStrategy(DbRouterConfig dbRouterConfig) {
        return new DbRouterStrategyHashCode(dbRouterConfig);
    }

    /**
     * 如果有就不注入
     * @param dbRouterConfig
     * @param dbRouterStrategy
     * @return
     */
    @Bean(name = "db-router-point")
    @ConditionalOnMissingBean
    public DbRouterJoinPoint point(DbRouterConfig dbRouterConfig, IDbRouterStrategy dbRouterStrategy) {
        return new DbRouterJoinPoint(dbRouterConfig, dbRouterStrategy);
    }

    @Bean
    public Interceptor plugin() {
        return new DynamicMyBatisPlugin();
    }

    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource) {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);

        TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setTransactionManager(dataSourceTransactionManager);
        transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return transactionTemplate;
    }

}
