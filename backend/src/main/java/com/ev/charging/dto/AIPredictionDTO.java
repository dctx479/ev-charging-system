package com.ev.charging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI预测相关DTO
 */
public class AIPredictionDTO {

    /**
     * 充电时长预测请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DurationPredictRequest implements Serializable {
        /**
         * 电池容量 (kWh)
         */
        @NotNull(message = "电池容量不能为空")
        @Min(value = 10, message = "电池容量最小为10kWh")
        @Max(value = 200, message = "电池容量最大为200kWh")
        @JsonProperty("battery_capacity")
        private BigDecimal batteryCapacity;

        /**
         * 当前SOC (%)
         */
        @NotNull(message = "当前电量不能为空")
        @Min(value = 0, message = "当前电量最小为0%")
        @Max(value = 100, message = "当前电量最大为100%")
        @JsonProperty("current_soc")
        private BigDecimal currentSoc;

        /**
         * 目标SOC (%)
         */
        @NotNull(message = "目标电量不能为空")
        @Min(value = 0, message = "目标电量最小为0%")
        @Max(value = 100, message = "目标电量最大为100%")
        @JsonProperty("target_soc")
        private BigDecimal targetSoc;

        /**
         * 充电功率 (kW)
         */
        @NotNull(message = "充电功率不能为空")
        @Min(value = 1, message = "充电功率最小为1kW")
        @Max(value = 500, message = "充电功率最大为500kW")
        @JsonProperty("charge_power")
        private BigDecimal chargePower;

        /**
         * 环境温度 (℃)，可选
         */
        @JsonProperty("temperature")
        private BigDecimal temperature;
    }

    /**
     * 充电时长预测响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DurationPredictResponse implements Serializable {
        /**
         * 预测充电时长（分钟）
         */
        @JsonProperty("duration")
        private BigDecimal duration;

        /**
         * 预计充电量（kWh）
         */
        @JsonProperty("charge_amount")
        private BigDecimal chargeAmount;

        /**
         * 预计费用（元）
         */
        @JsonProperty("estimated_cost")
        private BigDecimal estimatedCost;
    }

    /**
     * 故障预测请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaultPredictRequest implements Serializable {
        /**
         * 累计充电次数
         */
        @NotNull(message = "累计充电次数不能为空")
        @Min(value = 0, message = "累计充电次数不能为负数")
        @JsonProperty("total_charge_count")
        private Integer totalChargeCount;

        /**
         * 累计充电量（kWh）
         */
        @NotNull(message = "累计充电量不能为空")
        @Min(value = 0, message = "累计充电量不能为负数")
        @JsonProperty("total_charge_amount")
        private BigDecimal totalChargeAmount;

        /**
         * 距上次维护天数
         */
        @NotNull(message = "距上次维护天数不能为空")
        @Min(value = 0, message = "距上次维护天数不能为负数")
        @JsonProperty("days_since_last_maintenance")
        private Integer daysSinceLastMaintenance;

        /**
         * 健康度评分 (0-100)
         */
        @NotNull(message = "健康度评分不能为空")
        @Min(value = 0, message = "健康度评分最小为0")
        @Max(value = 100, message = "健康度评分最大为100")
        @JsonProperty("health_score")
        private Integer healthScore;

        /**
         * 平均每日使用次数
         */
        @NotNull(message = "平均每日使用次数不能为空")
        @Min(value = 0, message = "平均每日使用次数不能为负数")
        @JsonProperty("avg_daily_usage")
        private BigDecimal avgDailyUsage;

        /**
         * 电压波动
         */
        @NotNull(message = "电压波动不能为空")
        @Min(value = 0, message = "电压波动不能为负数")
        @JsonProperty("voltage_fluctuation")
        private BigDecimal voltageFluctuation;

        /**
         * 历史故障次数
         */
        @NotNull(message = "历史故障次数不能为空")
        @Min(value = 0, message = "历史故障次数不能为负数")
        @JsonProperty("fault_history_count")
        private Integer faultHistoryCount;
    }

    /**
     * 故障预测响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaultPredictResponse implements Serializable {
        /**
         * 故障概率 (0-100)
         */
        @JsonProperty("fault_probability")
        private BigDecimal faultProbability;

        /**
         * 是否会故障
         */
        @JsonProperty("will_fault")
        private Boolean willFault;

        /**
         * 建议
         */
        @JsonProperty("suggestion")
        private String suggestion;

        /**
         * 风险等级 (低/中/高)
         */
        private String riskLevel;

        /**
         * 根据故障概率计算风险等级
         */
        public void calculateRiskLevel() {
            if (faultProbability == null) {
                this.riskLevel = "未知";
                return;
            }

            double probability = faultProbability.doubleValue();
            if (probability >= 70) {
                this.riskLevel = "高风险";
            } else if (probability >= 40) {
                this.riskLevel = "中风险";
            } else {
                this.riskLevel = "低风险";
            }
        }
    }
}
