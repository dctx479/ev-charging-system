package com.ev.charging.controller.admin;

import com.ev.charging.common.Result;
import com.ev.charging.service.StatisticsService;
import com.ev.charging.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统计数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 获取仪表板数据
     */
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        log.info("查询仪表板数据");
        DashboardVO dashboard = statisticsService.getDashboardData();
        return Result.success(dashboard);
    }

    /**
     * 获取充电趋势（最近N天）
     */
    @GetMapping("/trend")
    public Result<ChargeTrendVO> getChargeTrend(@RequestParam(defaultValue = "7") Integer days) {
        log.info("查询充电趋势: days={}", days);
        ChargeTrendVO trend = statisticsService.getChargeTrend(days);
        return Result.success(trend);
    }

    /**
     * 获取充电桩状态分布
     */
    @GetMapping("/pile-status")
    public Result<PileStatusDistributionVO> getPileStatusDistribution() {
        log.info("查询充电桩状态分布");
        PileStatusDistributionVO distribution = statisticsService.getPileStatusDistribution();
        return Result.success(distribution);
    }

    /**
     * 获取站点收入排行
     */
    @GetMapping("/station-ranking")
    public Result<List<StationRankingVO>> getStationRanking(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("查询站点排行: limit={}", limit);
        List<StationRankingVO> ranking = statisticsService.getStationRanking(limit);
        return Result.success(ranking);
    }
}
