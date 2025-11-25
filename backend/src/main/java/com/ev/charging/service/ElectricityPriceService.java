package com.ev.charging.service;

import com.ev.charging.vo.ElectricityPriceVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class ElectricityPriceService {

    /**
     * 获取实时电价(模拟)
     * 实际应该调用国家电网API或第三方电价数据接口
     *
     * 谷时（23:00-07:00）: 0.4元/kWh
     * 平时（07:00-10:00, 15:00-18:00, 21:00-23:00）: 0.8元/kWh
     * 峰时（10:00-15:00, 18:00-21:00）: 1.2元/kWh
     */
    public BigDecimal getRealTimePrice() {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Shanghai"));
        int hour = now.getHour();

        // 峰时（10:00-15:00, 18:00-21:00）
        if ((hour >= 10 && hour < 15) || (hour >= 18 && hour < 21)) {
            return BigDecimal.valueOf(1.2);
        }
        // 谷时（23:00-07:00）
        if (hour >= 23 || hour < 7) {
            return BigDecimal.valueOf(0.4);
        }
        // 平时（07:00-10:00, 15:00-18:00, 21:00-23:00）
        return BigDecimal.valueOf(0.8);
    }

    /**
     * 获取未来24小时电价预测
     */
    public List<ElectricityPriceVO> get24HourForecast() {
        List<ElectricityPriceVO> forecast = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));

        for (int i = 0; i < 24; i++) {
            LocalDateTime time = now.plusHours(i);
            int hour = time.getHour();

            BigDecimal price;
            String priceLevel;

            // 峰时（10:00-15:00, 18:00-21:00）
            if ((hour >= 10 && hour < 15) || (hour >= 18 && hour < 21)) {
                price = BigDecimal.valueOf(1.2);
                priceLevel = "峰";
            }
            // 谷时（23:00-07:00）
            else if (hour >= 23 || hour < 7) {
                price = BigDecimal.valueOf(0.4);
                priceLevel = "谷";
            }
            // 平时（07:00-10:00, 15:00-18:00, 21:00-23:00）
            else {
                price = BigDecimal.valueOf(0.8);
                priceLevel = "平";
            }

            forecast.add(ElectricityPriceVO.builder()
                .time(time)
                .price(price)
                .priceLevel(priceLevel)
                .build()
            );
        }

        return forecast;
    }
}
