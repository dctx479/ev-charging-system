package com.ev.charging.config;

import com.ev.charging.datasource.DataSourceContextHolder;
import com.ev.charging.datasource.DynamicRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 多数据源配置
 * 配置主库、从库1、从库2三个数据源，并创建动态路由数据源
 * 
 * @author Architecture Team
 * @since 2026-01-23
 */
@Slf4j
@Configuration
@Profile("replication")
public class DataSourceConfig {
    
    /**
     * 主库数据源（写操作）
     */
    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        log.info("初始化主库数据源");
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }
    
    /**
     * 从库1数据源（读操作）
     */
    @Bean(name = "slave1DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.slave1")
    public DataSource slave1DataSource() {
        log.info("初始化从库1数据源");
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }
    
    /**
     * 从库2数据源（读操作）
     */
    @Bean(name = "slave2DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.slave2")
    public DataSource slave2DataSource() {
        log.info("初始化从库2数据源");
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }
    
    /**
     * 动态路由数据源（主数据源）
     * 根据ThreadLocal中的key动态选择目标数据源
     * 
     * @param masterDataSource 主库
     * @param slave1DataSource 从库1
     * @param slave2DataSource 从库2
     * @return 动态路由数据源
     */
    @Bean(name = "dynamicDataSource")
    @Primary
    public DataSource dynamicDataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("slave1DataSource") DataSource slave1DataSource,
            @Qualifier("slave2DataSource") DataSource slave2DataSource) {
        
        log.info("初始化动态路由数据源");
        
        DynamicRoutingDataSource dynamicDataSource = new DynamicRoutingDataSource();
        
        // 配置目标数据源映射
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceContextHolder.MASTER, masterDataSource);
        targetDataSources.put(DataSourceContextHolder.SLAVE1, slave1DataSource);
        targetDataSources.put(DataSourceContextHolder.SLAVE2, slave2DataSource);
        
        dynamicDataSource.setTargetDataSources(targetDataSources);
        
        // 设置默认数据源为主库（未设置数据源类型时使用）
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource);
        
        log.info("动态路由数据源初始化完成，默认数据源: master");
        
        return dynamicDataSource;
    }
}
