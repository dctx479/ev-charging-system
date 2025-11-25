package com.ev.charging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
    @Schema(description = "AI充电时长预测请求")
    public static class DurationPredictRequest implements Serializable {
        /**
         * 电池容量 (kWh)
         */
        @Schema(description = "电池容量（kWh）", example = "75.0", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "10", maximum = "200")
        @NotNull(message = "电池容量不能为空")
        @Min(value = 10, message = "电池容量最小为10kWh")
        @Max(value = 200, message = "电池容量最大为200kWh")
        @JsonProperty("battery_capacity")
        private BigDecimal batteryCapacity;

        /**
         * 当前SOC (%)
         */
        @Schema(description = "当前电量百分比（%）", example = "20", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0", maximum = "100")
        @NotNull(message = "当前电量不能为空")
        @Min(value = 0, message = "当前电量最小为0%")
        @Max(value = 100, message = "当前电量最大为100%")
        @JsonProperty("current_soc")
        private BigDecimal currentSoc;

        /**
         * 目标SOC (%)
         */
        @Schema(description = "目标电量百分比（%）", example = "80", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0", maximum = "100")
        @NotNull(message = "目标电量不能为空")
        @Min(value = 0, message = "目标电量最小为0%")
        @Max(value = 100, message = "目标电量最大为100%")
        @JsonProperty("target_soc")
        private BigDecimal targetSoc;

        /**
         * 充电功率 (kW)
         */
        @Schema(description = "充电功率（kW）", example = "120.0", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1", maximum = "500")
        @NotNull(message = "充电功率不能为空")
        @Min(value = 1, message = "充电功率最小为1kW")
        @Max(value = 500, message = "充电功率最大为500kW")
        @JsonProperty("charge_power")
        private BigDecimal chargePower;

        /**
         * 环境温度 (℃)，可选
         */
        @Schema(description = "环境温度（℃）", example = "25.0")
        @JsonProperty("temperature")
        private BigDecimal temperature;
    }

    /**
     * 充电时长预测响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI充电时长预测响应")
    public static class DurationPredictResponse implements Serializable {
        /**
         * 预测充电时长（分钟）
         */
        @Schema(description = "预测充电时长（分钟）", example = "45.5")
        @JsonProperty("duration")
        private BigDecimal duration;

        /**
         * 预计充电量（kWh）
         */
        @Schema(description = "预计充电量（kWh）", example = "45.0")
        @JsonProperty("charge_amount")
        private BigDecimal chargeAmount;

        /**
         * 预计费用（元）
         */
        @Schema(description = "预计充电费用（元）", example = "36.0")
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
    @Schema(description = "AI故障预测请求")
    public static class FaultPredictRequest implements Serializable {
        /**
         * 累计充电次数
         */
        @Schema(description = "累计充电次数", example = "5000", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0")
        @NotNull(message = "累计充电次数不能为空")
        @Min(value = 0, message = "累计充电次数不能为负数")
        @JsonProperty("total_charge_count")
        private Integer totalChargeCount;

        /**
         * 累计充电量（kWh）
         */
        @Schema(description = "累计充电量（kWh）", example = "250000.0", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0")
        @NotNull(message = "累计充电量不能为空")
        @Min(value = 0, message = "累计充电量不能为负数")
        @JsonProperty("total_charge_amount")
        private BigDecimal totalChargeAmount;

        /**
         * 距上次维护天数
         */
        @Schema(description = "距上次维护天数", example = "180", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0")
        @NotNull(message = "距上次维护天数不能为空")
        @Min(value = 0, message = "距上次维护天数不能为负数")
        @JsonProperty("days_since_last_maintenance")
        private Integer daysSinceLastMaintenance;

        /**
         * 健康度评分 (0-100)
         */
        @Schema(description = "充电桩健康度评分（0-100）", example = "75", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0", maximum = "100")
        @NotNull(message = "健康度评分不能为空")
        @Min(value = 0, message = "健康度评分最小为0")
        @Max(value = 100, message = "健康度评分最大为100")
        @JsonProperty("health_score")
        private Integer healthScore;

        /**
         * 平均每日使用次数
         */
        @Schema(description = "平均每日使用次数", example = "28.5", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0")
        @NotNull(message = "平均每日使用次数不能为空")
        @Min(value = 0, message = "平均每日使用次数不能为负数")
        @JsonProperty("avg_daily_usage")
        private BigDecimal avgDailyUsage;

        /**
         * 电压波动
         */
        @Schema(description = "电压波动值", example = "3.5", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0")
        @NotNull(message = "电压波动不能为空")
        @Min(value = 0, message = "电压波动不能为负数")
        @JsonProperty("voltage_fluctuation")
        private BigDecimal voltageFluctuation;

        /**
         * 历史故障次数
         */
        @Schema(description = "历史故障次数", example = "3", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0")
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
    @Schema(description = "AI故障预测响应")
    public static class FaultPredictResponse implements Serializable {
        /**
         * 故障概率 (0-100)
         */
        @Schema(description = "故障概率（0-100）", example = "75.5")
        @JsonProperty("fault_probability")
        private BigDecimal faultProbability;

        /**
         * 是否会故障
         */
        @Schema(description = "是否预测会发生故障", example = "true")
        @JsonProperty("will_fault")
        private Boolean willFault;

        /**
         * 建议
         */
        @Schema(description = "维护建议", example = "建议尽快安排预防性维护")
        @JsonProperty("suggestion")
        private String suggestion;

        /**
         * 风险等级 (低/中/高)
         */
        @Schema(description = "风险等级", example = "高风险", allowableValues = {"低风险", "中风险", "高风险", "未知"})
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
