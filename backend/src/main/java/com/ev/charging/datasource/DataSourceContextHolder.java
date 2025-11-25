package com.ev.charging.datasource;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据源上下文持有者
 * 使用ThreadLocal存储当前线程使用的数据源类型
 * 
 * @author Architecture Team
 * @since 2026-01-23
 */
@Slf4j
public class DataSourceContextHolder {
    
    /**
     * 数据源类型枚举
     */
    public static final String MASTER = "master";
    public static final String SLAVE1 = "slave1";
    public static final String SLAVE2 = "slave2";
    
    /**
     * ThreadLocal存储当前线程的数据源类型
     */
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();
    
    /**
     * 轮询计数器，用于从库负载均衡
     */
    private static final AtomicInteger counter = new AtomicInteger(0);
    
    /**
     * 设置使用主库（写操作）
     */
    public static void setMaster() {
        CONTEXT_HOLDER.set(MASTER);
        log.debug("切换到主库: {}", MASTER);
    }
    
    /**
     * 设置使用从库（读操作）
     * 使用轮询策略在多个从库之间负载均衡
     */
    public static void setSlave() {
        // 轮询选择从库：slave1 -> slave2 -> slave1 ...
        int index = counter.getAndIncrement() % 2;
        String slave = (index == 0) ? SLAVE1 : SLAVE2;
        CONTEXT_HOLDER.set(slave);
        log.debug("切换到从库: {}", slave);
    }
    
    /**
     * 手动指定数据源
     * 
     * @param dataSourceKey 数据源key (master/slave1/slave2)
     */
    public static void setDataSource(String dataSourceKey) {
        if (MASTER.equals(dataSourceKey) || SLAVE1.equals(dataSourceKey) || SLAVE2.equals(dataSourceKey)) {
            CONTEXT_HOLDER.set(dataSourceKey);
            log.debug("手动切换到数据源: {}", dataSourceKey);
        } else {
            log.warn("无效的数据源key: {}, 使用默认主库", dataSourceKey);
            CONTEXT_HOLDER.set(MASTER);
        }
    }
    
    /**
     * 获取当前线程的数据源类型
     * 
     * @return 数据源key，如果未设置则返回null（将使用默认数据源）
     */
    public static String getDataSourceType() {
        String dataSource = CONTEXT_HOLDER.get();
        log.debug("获取当前数据源: {}", dataSource);
        return dataSource;
    }
    
    /**
     * 清除当前线程的数据源设置
     * 必须在请求结束时调用，防止ThreadLocal内存泄漏
     */
    public static void clear() {
        String dataSource = CONTEXT_HOLDER.get();
        CONTEXT_HOLDER.remove();
        log.debug("清除数据源上下文: {}", dataSource);
    }
}
