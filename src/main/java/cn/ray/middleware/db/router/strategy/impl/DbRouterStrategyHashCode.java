package cn.ray.middleware.db.router.strategy.impl;

import cn.ray.middleware.db.router.DbContextHolder;
import cn.ray.middleware.db.router.DbRouterConfig;
import cn.ray.middleware.db.router.strategy.IDbRouterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ray
 * @date 2022/10/18 14:48
 * @description 哈希路由
 */
public class DbRouterStrategyHashCode implements IDbRouterStrategy {

    private Logger logger = LoggerFactory.getLogger(DbRouterStrategyHashCode.class);

    private DbRouterConfig dbRouterConfig;

    public DbRouterStrategyHashCode(DbRouterConfig dbRouterConfig) {
        this.dbRouterConfig = dbRouterConfig;
    }

    @Override
    public void dbRouter(String dbKeyAttr) {
        int size = dbRouterConfig.getDbCount() + dbRouterConfig.getTbCount();

        // 扰动函数；在 JDK 的 HashMap 中，对于一个元素的存放，需要进行哈希散列。而为了让散列更加均匀，添加了扰动函数
        int idx = (size - 1) & (dbKeyAttr.hashCode()^(dbKeyAttr.hashCode() >>> 16));

        // 库表索引
        int dbIdx = idx / dbRouterConfig.getDbCount() - 1;
        int tbIdx = idx - dbRouterConfig.getTbCount() * (dbIdx - 1);

        // 设置到 ThreadLocal
        DbContextHolder.setDBKey(String.format("%02d",dbIdx));
        DbContextHolder.setTBKey(String.format("%03d",tbIdx));

        logger.debug("数据库路由: dbIdx {} , tbIdx {}",dbIdx,tbIdx);
    }

    @Override
    public void setDbKey(int dbIdx) {
        DbContextHolder.setDBKey(String.format("%02d",dbIdx));
    }

    @Override
    public void setTbKey(int tbIdx) {
        DbContextHolder.setTBKey(String.format("%03d",tbIdx));
    }

    @Override
    public int dbCount() {
        return dbRouterConfig.getDbCount();
    }

    @Override
    public int tbCount() {
        return dbRouterConfig.getTbCount();
    }

    @Override
    public void clear() {
        DbContextHolder.clearDBKey();
        DbContextHolder.clearTBKey();
    }
}
