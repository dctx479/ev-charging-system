#!/bin/bash
# ==========================================
# 新能源汽车充电系统性能压力测试脚本
# 版本: v1.0
# 日期: 2026-01-23
# ==========================================

# 配置
BASE_URL="${BASE_URL:-http://localhost:8080/api}"
CONCURRENT_LIGHT=100
CONCURRENT_MEDIUM=200
CONCURRENT_HEAVY=50
DURATION_SHORT=180   # 3分钟
DURATION_MEDIUM=300  # 5分钟

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志文件
REPORT_DIR="/tmp/performance_test_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$REPORT_DIR"
LOG_FILE="$REPORT_DIR/test.log"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

# 检查依赖
check_dependencies() {
    log_info "检查依赖..."

    if ! command -v curl &> /dev/null; then
        log_error "curl 未安装，请先安装 curl"
        exit 1
    fi

    if ! command -v bc &> /dev/null; then
        log_warn "bc 未安装，部分统计功能可能无法使用"
    fi

    log_success "依赖检查完成"
}

# 检查服务可用性
check_service() {
    log_info "检查服务可用性: $BASE_URL"

    # 尝试访问健康检查接口
    response=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "$BASE_URL/auth/login" || echo "000")

    if [ "$response" = "000" ]; then
        log_error "无法连接到服务: $BASE_URL"
        log_error "请确保后端服务已启动"
        exit 1
    fi

    log_success "服务可用 (HTTP $response)"
}

# 场景1: 附近充电站查询
test_nearby_stations() {
    log_info "========================================="
    log_info "场景1: 附近充电站查询"
    log_info "并发数: $CONCURRENT_LIGHT, 持续时间: $DURATION_MEDIUM秒"
    log_info "========================================="

    local result_file="$REPORT_DIR/scenario1.txt"
    local error_file="$REPORT_DIR/scenario1_errors.txt"

    # 清空结果文件
    > "$result_file"
    > "$error_file"

    local test_url="$BASE_URL/stations/nearby?lng=116.404&lat=39.915&radius=5"

    log_info "启动 $CONCURRENT_LIGHT 个并发线程..."

    for i in $(seq 1 $CONCURRENT_LIGHT); do
        (
            local count=0
            local errors=0
            local start_time=$(date +%s)
            local end_time=$((start_time + DURATION_MEDIUM))

            while [ $(date +%s) -lt $end_time ]; do
                local req_start=$(date +%s.%3N)

                # 发送请求
                http_code=$(curl -s -o /dev/null -w "%{http_code}" \
                    --connect-timeout 5 \
                    --max-time 10 \
                    "$test_url" 2>/dev/null)

                local req_end=$(date +%s.%3N)
                local response_time=$(echo "$req_end - $req_start" | bc)

                if [ "$http_code" = "200" ]; then
                    echo "$response_time" >> "$result_file"
                else
                    echo "HTTP $http_code" >> "$error_file"
                    errors=$((errors + 1))
                fi

                count=$((count + 1))

                # 短暂休息避免CPU占满
                sleep 0.01
            done

            log_info "线程 $i 完成: $count 次请求, $errors 次错误"
        ) &
    done

    # 等待所有线程完成
    wait

    # 统计结果
    analyze_results "$result_file" "$error_file" "场景1: 附近充电站查询"
}

# 场景2: 充电桩状态查询
test_pile_status() {
    log_info "========================================="
    log_info "场景2: 充电桩状态查询"
    log_info "并发数: $CONCURRENT_MEDIUM, 持续时间: $DURATION_MEDIUM秒"
    log_info "========================================="

    local result_file="$REPORT_DIR/scenario2.txt"
    local error_file="$REPORT_DIR/scenario2_errors.txt"

    > "$result_file"
    > "$error_file"

    log_info "启动 $CONCURRENT_MEDIUM 个并发线程..."

    for i in $(seq 1 $CONCURRENT_MEDIUM); do
        (
            local count=0
            local errors=0
            local start_time=$(date +%s)
            local end_time=$((start_time + DURATION_MEDIUM))

            while [ $(date +%s) -lt $end_time ]; do
                # 随机选择充电站ID (1-100)
                local station_id=$((RANDOM % 100 + 1))
                local test_url="$BASE_URL/piles/${station_id}"

                local req_start=$(date +%s.%3N)

                http_code=$(curl -s -o /dev/null -w "%{http_code}" \
                    --connect-timeout 5 \
                    --max-time 10 \
                    "$test_url" 2>/dev/null)

                local req_end=$(date +%s.%3N)
                local response_time=$(echo "$req_end - $req_start" | bc)

                if [ "$http_code" = "200" ]; then
                    echo "$response_time" >> "$result_file"
                else
                    echo "HTTP $http_code" >> "$error_file"
                    errors=$((errors + 1))
                fi

                count=$((count + 1))
                sleep 0.01
            done

            log_info "线程 $i 完成: $count 次请求, $errors 次错误"
        ) &
    done

    wait

    analyze_results "$result_file" "$error_file" "场景2: 充电桩状态查询"
}

# 场景3: 用户登录
test_user_login() {
    log_info "========================================="
    log_info "场景3: 用户登录"
    log_info "并发数: $CONCURRENT_LIGHT, 持续时间: $DURATION_SHORT秒"
    log_info "========================================="

    local result_file="$REPORT_DIR/scenario3.txt"
    local error_file="$REPORT_DIR/scenario3_errors.txt"

    > "$result_file"
    > "$error_file"

    local test_url="$BASE_URL/auth/login"

    log_info "启动 $CONCURRENT_LIGHT 个并发线程..."

    for i in $(seq 1 $CONCURRENT_LIGHT); do
        (
            local count=0
            local errors=0
            local start_time=$(date +%s)
            local end_time=$((start_time + DURATION_SHORT))

            while [ $(date +%s) -lt $end_time ]; do
                # 模拟登录请求（需要根据实际API调整）
                local req_start=$(date +%s.%3N)

                http_code=$(curl -s -o /dev/null -w "%{http_code}" \
                    --connect-timeout 5 \
                    --max-time 10 \
                    -X POST \
                    -H "Content-Type: application/json" \
                    -d '{"phone":"13800000001","password":"123456"}' \
                    "$test_url" 2>/dev/null)

                local req_end=$(date +%s.%3N)
                local response_time=$(echo "$req_end - $req_start" | bc)

                if [ "$http_code" = "200" ] || [ "$http_code" = "401" ]; then
                    # 401也算成功响应（密码错误）
                    echo "$response_time" >> "$result_file"
                else
                    echo "HTTP $http_code" >> "$error_file"
                    errors=$((errors + 1))
                fi

                count=$((count + 1))
                sleep 0.01
            done

            log_info "线程 $i 完成: $count 次请求, $errors 次错误"
        ) &
    done

    wait

    analyze_results "$result_file" "$error_file" "场景3: 用户登录"
}

# 结果分析
analyze_results() {
    local result_file="$1"
    local error_file="$2"
    local scenario_name="$3"

    log_info "-----------------------------------------"
    log_info "分析 $scenario_name 结果..."

    if [ ! -f "$result_file" ] || [ ! -s "$result_file" ]; then
        log_error "结果文件为空或不存在"
        return
    fi

    local total_requests=$(wc -l < "$result_file")
    local total_errors=0

    if [ -f "$error_file" ]; then
        total_errors=$(wc -l < "$error_file")
    fi

    local success_requests=$total_requests
    local total_all=$((total_requests + total_errors))

    log_info "总请求数: $total_all"
    log_info "成功请求: $success_requests"
    log_info "失败请求: $total_errors"

    if [ $total_all -gt 0 ]; then
        local success_rate=$(echo "scale=2; $success_requests * 100 / $total_all" | bc)
        local error_rate=$(echo "scale=2; $total_errors * 100 / $total_all" | bc)
        log_info "成功率: ${success_rate}%"
        log_info "错误率: ${error_rate}%"
    fi

    # 使用awk计算统计数据
    awk '
    BEGIN {
        count = 0
        sum = 0
        min = 999999
        max = 0
    }
    {
        count++
        sum += $1
        if ($1 < min) min = $1
        if ($1 > max) max = $1
        arr[count] = $1
    }
    END {
        if (count == 0) {
            print "没有有效数据"
            exit
        }

        # 排序
        asort(arr)

        avg = sum / count
        p50 = arr[int(count * 0.5)]
        p90 = arr[int(count * 0.9)]
        p95 = arr[int(count * 0.95)]
        p99 = arr[int(count * 0.99)]

        printf "\n=== 响应时间统计 (秒) ===\n"
        printf "平均响应时间: %.3f (%.0fms)\n", avg, avg * 1000
        printf "P50响应时间: %.3f (%.0fms)\n", p50, p50 * 1000
        printf "P90响应时间: %.3f (%.0fms)\n", p90, p90 * 1000
        printf "P95响应时间: %.3f (%.0fms)\n", p95, p95 * 1000
        printf "P99响应时间: %.3f (%.0fms)\n", p99, p99 * 1000
        printf "最小响应时间: %.3f (%.0fms)\n", min, min * 1000
        printf "最大响应时间: %.3f (%.0fms)\n", max, max * 1000

        # 假设测试持续300秒
        tps = count / 300
        printf "\n吞吐量: %.2f TPS (请求/秒)\n", tps
    }
    ' "$result_file" | tee -a "$LOG_FILE"

    log_success "$scenario_name 分析完成"
    log_info "-----------------------------------------"
}

# 获取缓存统计
get_cache_stats() {
    log_info "========================================="
    log_info "获取缓存统计信息"
    log_info "========================================="

    local cache_stats_url="$BASE_URL/admin/performance/cache-stats"

    log_info "请求: $cache_stats_url"

    local response=$(curl -s "$cache_stats_url" 2>/dev/null)

    if [ $? -eq 0 ]; then
        echo "$response" | tee -a "$LOG_FILE"
        echo "$response" > "$REPORT_DIR/cache_stats.json"
        log_success "缓存统计获取成功"
    else
        log_error "无法获取缓存统计"
    fi
}

# 生成测试报告
generate_report() {
    log_info "========================================="
    log_info "生成测试报告"
    log_info "========================================="

    local report_file="$REPORT_DIR/SUMMARY.md"

    cat > "$report_file" << EOF
# 性能压力测试报告摘要

**测试时间**: $(date '+%Y-%m-%d %H:%M:%S')
**测试环境**: $BASE_URL
**测试工具**: Bash + Curl
**报告目录**: $REPORT_DIR

## 测试场景

### 场景1: 附近充电站查询
- 并发数: $CONCURRENT_LIGHT
- 持续时间: $DURATION_MEDIUM 秒
- 结果文件: scenario1.txt

### 场景2: 充电桩状态查询
- 并发数: $CONCURRENT_MEDIUM
- 持续时间: $DURATION_MEDIUM 秒
- 结果文件: scenario2.txt

### 场景3: 用户登录
- 并发数: $CONCURRENT_LIGHT
- 持续时间: $DURATION_SHORT 秒
- 结果文件: scenario3.txt

## 详细结果

详细统计数据请查看 test.log 文件。

## 缓存统计

详细缓存统计请查看 cache_stats.json 文件。

---
**生成时间**: $(date '+%Y-%m-%d %H:%M:%S')
EOF

    log_success "测试报告已生成: $report_file"
    log_info "报告目录: $REPORT_DIR"
}

# 主函数
main() {
    echo ""
    echo "========================================="
    echo "   新能源汽车充电系统性能压力测试"
    echo "========================================="
    echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "测试环境: $BASE_URL"
    echo "报告目录: $REPORT_DIR"
    echo "========================================="
    echo ""

    # 检查依赖
    check_dependencies

    # 检查服务
    check_service

    # 执行测试场景
    log_info "开始执行性能测试..."

    # 场景1: 附近充电站查询
    test_nearby_stations

    # 短暂休息
    log_info "休息30秒后继续..."
    sleep 30

    # 场景2: 充电桩状态查询
    test_pile_status

    # 短暂休息
    log_info "休息30秒后继续..."
    sleep 30

    # 场景3: 用户登录
    test_user_login

    # 获取缓存统计
    log_info "休息10秒后获取缓存统计..."
    sleep 10
    get_cache_stats

    # 生成报告
    generate_report

    echo ""
    echo "========================================="
    echo "   性能测试完成！"
    echo "========================================="
    echo "报告目录: $REPORT_DIR"
    echo "查看日志: cat $LOG_FILE"
    echo "查看摘要: cat $REPORT_DIR/SUMMARY.md"
    echo "========================================="
    echo ""
}

# 清理函数
cleanup() {
    log_warn "测试被中断，正在清理..."
    # 杀死所有子进程
    pkill -P $$
    exit 1
}

# 捕获中断信号
trap cleanup SIGINT SIGTERM

# 执行主函数
main "$@"
