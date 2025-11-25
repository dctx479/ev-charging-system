@echo off
REM ###############################################################################
REM EV Charging System - 监控系统部署脚本 (Windows)
REM 用途: 部署 Prometheus, Grafana, Alertmanager 监控栈
REM ###############################################################################

setlocal enabledelayedexpansion

echo ==========================================
echo   EV Charging System - 监控系统部署
echo ==========================================
echo.

REM 检查 Docker
echo [INFO] 检查 Docker 环境...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker 未安装，请先安装 Docker Desktop
    pause
    exit /b 1
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Compose 未安装，请先安装 Docker Compose
    pause
    exit /b 1
)

echo [INFO] Docker 环境检查通过
echo.

REM 检查配置文件
echo [INFO] 检查配置文件...

set "REQUIRED_FILES=prometheus\prometheus.yml prometheus\rules\alerts.yml alertmanager\alertmanager.yml grafana\provisioning\datasources\prometheus.yml grafana\provisioning\dashboards\dashboards.yml grafana\dashboards\ev-charging-dashboard.json docker-compose.yml"

for %%f in (%REQUIRED_FILES%) do (
    if not exist "%%f" (
        echo [ERROR] 配置文件不存在: %%f
        pause
        exit /b 1
    )
)

echo [INFO] 配置文件检查通过
echo.

REM 创建环境变量文件
if not exist ".env" (
    echo [WARN] .env 文件不存在，从 .env.example 复制...
    copy .env.example .env >nul
    echo [INFO] 已创建 .env 文件，请根据实际情况修改配置
)

REM 停止旧容器
echo [INFO] 停止旧容器...
docker-compose down 2>nul

REM 拉取最新镜像
echo [INFO] 拉取最新镜像...
docker-compose pull

REM 启动监控栈
echo [INFO] 启动监控服务...
docker-compose up -d

REM 等待服务启动
echo [INFO] 等待服务启动...
timeout /t 10 /nobreak >nul

REM 验证服务状态
echo [INFO] 验证服务状态...

set ALL_HEALTHY=true

REM 检查 Prometheus
echo [INFO] 检查 Prometheus...
set RETRY=0
:CHECK_PROMETHEUS
curl -sf http://localhost:9090/-/healthy >nul 2>&1
if errorlevel 1 (
    set /a RETRY+=1
    if !RETRY! LSS 30 (
        timeout /t 2 /nobreak >nul
        goto CHECK_PROMETHEUS
    ) else (
        echo [ERROR] Prometheus 服务启动失败
        set ALL_HEALTHY=false
    )
) else (
    echo [INFO] Prometheus 服务正常 ✓
)

REM 检查 Grafana
echo [INFO] 检查 Grafana...
set RETRY=0
:CHECK_GRAFANA
curl -sf http://localhost:3000/api/health >nul 2>&1
if errorlevel 1 (
    set /a RETRY+=1
    if !RETRY! LSS 30 (
        timeout /t 2 /nobreak >nul
        goto CHECK_GRAFANA
    ) else (
        echo [ERROR] Grafana 服务启动失败
        set ALL_HEALTHY=false
    )
) else (
    echo [INFO] Grafana 服务正常 ✓
)

REM 检查 Alertmanager
echo [INFO] 检查 Alertmanager...
set RETRY=0
:CHECK_ALERTMANAGER
curl -sf http://localhost:9093/-/healthy >nul 2>&1
if errorlevel 1 (
    set /a RETRY+=1
    if !RETRY! LSS 30 (
        timeout /t 2 /nobreak >nul
        goto CHECK_ALERTMANAGER
    ) else (
        echo [ERROR] Alertmanager 服务启动失败
        set ALL_HEALTHY=false
    )
) else (
    echo [INFO] Alertmanager 服务正常 ✓
)

echo.
echo ==========================================
echo   监控系统部署完成
echo ==========================================
echo.
echo 服务访问地址:
echo   Prometheus:    http://localhost:9090
echo   Grafana:       http://localhost:3000 (admin/admin123)
echo   Alertmanager:  http://localhost:9093
echo.
echo Exporter 指标地址:
echo   MySQL Exporter:  http://localhost:9104/metrics
echo   Redis Exporter:  http://localhost:9121/metrics
echo   Node Exporter:   http://localhost:9100/metrics
echo.
echo 后端应用指标:
echo   Spring Boot:     http://localhost:8080/api/actuator/prometheus
echo.

REM 显示容器状态
echo 容器状态:
docker-compose ps
echo.

echo 后续操作:
echo   1. 访问 Grafana (http://localhost:3000) 查看监控 Dashboard
echo   2. 确认后端应用已启动并暴露 /actuator/prometheus 端点
echo   3. 在 Alertmanager 中配置告警通知渠道（邮件/钉钉/企业微信）
echo   4. 根据需要调整 prometheus\rules\alerts.yml 中的告警阈值
echo.

if "%ALL_HEALTHY%"=="false" (
    echo [WARN] 部分服务启动失败，请检查日志:
    echo   docker-compose logs [service-name]
    pause
    exit /b 1
)

echo [INFO] 部署成功！ ✓
pause
