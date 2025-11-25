@echo off
REM ###############################################################################
REM EV Charging System - 监控系统停止脚本 (Windows)
REM 用途: 停止所有监控服务
REM ###############################################################################

echo ==========================================
echo   EV Charging System - 停止监控系统
echo ==========================================
echo.

set /p DELETE_VOLUMES="是否删除数据卷（包括历史指标数据）？[y/N]: "

if /i "%DELETE_VOLUMES%"=="y" (
    echo [WARN] 停止服务并删除数据卷...
    docker-compose down -v
    echo [INFO] 服务已停止，数据卷已删除
) else (
    echo [INFO] 停止服务（保留数据卷）...
    docker-compose down
    echo [INFO] 服务已停止，数据卷已保留
)

echo.
echo [INFO] 监控系统已停止
pause
