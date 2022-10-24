package cn.ray.middleware.db.router.dynamic;

import cn.ray.middleware.db.router.DbContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @author Ray
 * @date 2022/10/18 14:05
 * @description 动态数据源获取,每当切换数据源,都需要从这里获取
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return "db" + DbContextHolder.getDBKey();
    }

}
