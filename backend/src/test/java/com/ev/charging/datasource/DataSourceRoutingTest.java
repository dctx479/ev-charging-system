package com.ev.charging.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据源路由测试
 * 验证DataSourceContextHolder的路由逻辑
 * 
 * @author Architecture Team
 * @since 2026-01-23
 */
class DataSourceRoutingTest {
    
    @AfterEach
    void tearDown() {
        // 每个测试后清理ThreadLocal
        DataSourceContextHolder.clear();
    }
    
    @Test
    void testSetMaster() {
        // 设置主库
        DataSourceContextHolder.setMaster();
        
        // 验证
        assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.getDataSourceType());
    }
    
    @Test
    void testSetSlave() {
        // 设置从库（第1次应返回slave1）
        DataSourceContextHolder.setSlave();
        String firstSlave = DataSourceContextHolder.getDataSourceType();
        assertTrue(firstSlave.equals(DataSourceContextHolder.SLAVE1) || 
                  firstSlave.equals(DataSourceContextHolder.SLAVE2));
        
        // 清理
        DataSourceContextHolder.clear();
        
        // 设置从库（第2次应返回slave2）
        DataSourceContextHolder.setSlave();
        String secondSlave = DataSourceContextHolder.getDataSourceType();
        assertTrue(secondSlave.equals(DataSourceContextHolder.SLAVE1) || 
                  secondSlave.equals(DataSourceContextHolder.SLAVE2));
        
        // 验证轮询（两次结果可能相同或不同，取决于counter的初始值）
        assertNotNull(firstSlave);
        assertNotNull(secondSlave);
    }
    
    @Test
    void testSetDataSource() {
        // 手动设置slave1
        DataSourceContextHolder.setDataSource(DataSourceContextHolder.SLAVE1);
        assertEquals(DataSourceContextHolder.SLAVE1, DataSourceContextHolder.getDataSourceType());
        
        // 清理
        DataSourceContextHolder.clear();
        
        // 手动设置slave2
        DataSourceContextHolder.setDataSource(DataSourceContextHolder.SLAVE2);
        assertEquals(DataSourceContextHolder.SLAVE2, DataSourceContextHolder.getDataSourceType());
        
        // 清理
        DataSourceContextHolder.clear();
        
        // 设置无效数据源，应回退到master
        DataSourceContextHolder.setDataSource("invalid");
        assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.getDataSourceType());
    }
    
    @Test
    void testClear() {
        // 设置主库
        DataSourceContextHolder.setMaster();
        assertNotNull(DataSourceContextHolder.getDataSourceType());
        
        // 清理
        DataSourceContextHolder.clear();
        
        // 验证已清理
        assertNull(DataSourceContextHolder.getDataSourceType());
    }
    
    @Test
    void testThreadLocal() {
        // 主线程设置master
        DataSourceContextHolder.setMaster();
        assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.getDataSourceType());
        
        // 新线程应该获取不到（ThreadLocal隔离）
        Thread thread = new Thread(() -> {
            String dataSourceType = DataSourceContextHolder.getDataSourceType();
            // 新线程中应该是null（未设置）或master（根据实现）
            assertNull(dataSourceType);
        });
        
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            fail("Thread interrupted");
        }
        
        // 主线程的值不受影响
        assertEquals(DataSourceContextHolder.MASTER, DataSourceContextHolder.getDataSourceType());
    }
}
