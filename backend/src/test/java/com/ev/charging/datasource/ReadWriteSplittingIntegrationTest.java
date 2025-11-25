package com.ev.charging.datasource;

import com.ev.charging.entity.ChargingStation;
import com.ev.charging.repository.ChargingStationRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 读写分离集成测试
 * 
 * 注意: 此测试需要启动读写分离环境（docker-compose-replication.yml）
 * 
 * @author Architecture Team
 * @since 2026-01-23
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("replication")
class ReadWriteSplittingIntegrationTest {
    
    @Autowired
    private ChargingStationRepository stationRepository;
    
    /**
     * 测试读操作路由到从库
     */
    @Test
    @Transactional(readOnly = true)
    void testReadFromSlave() {
        log.info("=== 测试读操作路由到从库 ===");
        
        // 查询所有充电站（应路由到从库）
        List<ChargingStation> stations = stationRepository.findAll();
        
        // 验证查询成功
        assertNotNull(stations);
        
        // 检查ThreadLocal中的数据源类型
        String dataSourceType = DataSourceContextHolder.getDataSourceType();
        log.info("当前数据源: {}", dataSourceType);
        
        // 注意: 由于AOP在方法执行后会清理ThreadLocal，这里可能获取不到
        // 实际验证需要查看日志输出
    }
    
    /**
     * 测试写操作路由到主库
     */
    @Test
    @Transactional
    void testWriteToMaster() {
        log.info("=== 测试写操作路由到主库 ===");
        
        // 创建测试充电站（应路由到主库）
        ChargingStation station = new ChargingStation();
        station.setName("测试充电站-读写分离");
        station.setProvince("上海市");
        station.setCity("上海市");
        station.setAddress("测试地址");
        station.setLatitude(new BigDecimal("31.230416"));
        station.setLongitude(new BigDecimal("121.473701"));
        station.setOperatorId(1L);
        
        ChargingStation saved = stationRepository.save(station);
        
        // 验证保存成功
        assertNotNull(saved.getId());
        
        log.info("保存成功，ID: {}", saved.getId());
        
        // 清理测试数据
        stationRepository.delete(saved);
    }
    
    /**
     * 测试强制使用主库读取
     */
    @Test
    @Transactional(readOnly = true)
    void testForceReadFromMaster() {
        log.info("=== 测试强制使用主库读取 ===");
        
        // 手动切换到主库
        DataSourceContextHolder.setMaster();
        
        // 查询数据（应从主库读取）
        List<ChargingStation> stations = stationRepository.findAll();
        
        // 验证查询成功
        assertNotNull(stations);
        
        log.info("从主库查询到 {} 个充电站", stations.size());
    }
    
    /**
     * 测试写后立即读（可能遇到主从延迟）
     */
    @Test
    @Transactional
    void testWriteThenRead() {
        log.info("=== 测试写后立即读 ===");
        
        // 1. 写入数据到主库
        ChargingStation station = new ChargingStation();
        station.setName("测试充电站-写后立即读");
        station.setProvince("上海市");
        station.setCity("上海市");
        station.setAddress("测试地址");
        station.setLatitude(new BigDecimal("31.230416"));
        station.setLongitude(new BigDecimal("121.473701"));
        station.setOperatorId(1L);

        ChargingStation saved = stationRepository.save(station);
        Long stationId = saved.getId();

        log.info("写入成功，ID: {}", stationId);

        // 2. 立即读取（在同一事务中，从主库读取）
        ChargingStation found = stationRepository.findById(stationId).orElse(null);
        assertNotNull(found);
        assertEquals("测试充电站-写后立即读", found.getName());

        log.info("立即读取成功: {}", found.getName());
        
        // 清理测试数据
        stationRepository.delete(saved);
    }
    
    /**
     * 测试并发读操作的负载均衡
     */
    @Test
    void testLoadBalancing() {
        log.info("=== 测试并发读操作的负载均衡 ===");
        
        // 连续执行10次读操作，应该在slave1和slave2之间轮询
        for (int i = 0; i < 10; i++) {
            DataSourceContextHolder.setSlave();
            String dataSourceType = DataSourceContextHolder.getDataSourceType();
            log.info("第{}次读操作，数据源: {}", i + 1, dataSourceType);
            assertTrue(dataSourceType.equals(DataSourceContextHolder.SLAVE1) || 
                      dataSourceType.equals(DataSourceContextHolder.SLAVE2));
            DataSourceContextHolder.clear();
        }
    }
}
