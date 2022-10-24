package cn.ray.middleware.db.router.strategy;

/**
 * @author Ray
 * @date 2022/10/18 14:31
 * @description 路由策略
 */
public interface IDbRouterStrategy {

    /**
     * 路由计算
     * @param dbKeyAttr 路由字段s
     */
    void dbRouter(String dbKeyAttr);

    /**
     * 手动配置分库路由
     * @param dbIdx 路由库
     */
    void setDbKey(int dbIdx);

    /**
     * 手动配置分表路由
     * @param tbIdx 路由表
     */
    void setTbKey(int tbIdx);

    /**
     * 获取分库数
     * @return 分库数
     */
    int dbCount();

    /**
     * 获取分表数
     * @return 分表数
     */
    int tbCount();

    /**
     * 清除路由
     */
    void clear();
}
