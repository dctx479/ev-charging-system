package com.ev.charging.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态路由数据源
 * 继承AbstractRoutingDataSource，根据ThreadLocal中的key选择目标数据源
 * 
 * @author Architecture Team
 * @since 2026-01-23
 */
@Slf4j
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {
    
    /**
     * 决定使用哪个数据源
     * Spring会在每次数据库操作前调用此方法
     * 
     * @return 数据源key (master/slave1/slave2)
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String dataSourceType = DataSourceContextHolder.getDataSourceType();
        
        // 如果未设置，返回null，将使用默认数据源（master）
        if (dataSourceType == null) {
            log.debug("未设置数据源类型，将使用默认主库");
            return DataSourceContextHolder.MASTER;
        }
        
        log.debug("路由到数据源: {}", dataSourceType);
        return dataSourceType;
    }
}
