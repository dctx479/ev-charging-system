@echo off
REM EV充电站点管理系统 - 后端服务启动脚本 (Windows)
REM 版本: 1.0.0
REM 用途: 一键启动后端服务

setlocal enabledelayedexpansion

REM 配置
set PROJECT_DIR=%cd%
set BACKEND_DIR=%PROJECT_DIR%\backend
set LOG_FILE=%BACKEND_DIR%\startup.log

REM 清空日志
if exist "%LOG_FILE%" del "%LOG_FILE%"

REM 标题
cls
echo.
echo ========================================================================
echo  EV充电站点管理系统 - 后端服务启动脚本
echo  版本: 1.0.0
echo  启动时间: %date% %time%
echo ========================================================================
echo.

REM 检查环境
echo [INFO] 正在检查环境...

REM 检查Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java未安装
    pause
    exit /b 1
)
for /f "tokens=*" %%i in ('java -version 2^>^&1 ^| findstr version') do set JAVA_VERSION=%%i
echo [SUCCESS] Java已安装: %JAVA_VERSION%

REM 检查Maven Wrapper
if not exist "%BACKEND_DIR%\mvnw.cmd" (
    echo [ERROR] Maven Wrapper不存在
    pause
    exit /b 1
)
echo [SUCCESS] Maven Wrapper已就绪

REM 检查MySQL (可选)
echo [INFO] 正在检查MySQL...
mysql -h localhost -u root -p"%DB_PASSWORD:root123456%" -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
    echo [WARN] MySQL连接失败，请确保MySQL已启动
) else (
    echo [SUCCESS] MySQL连接成功
)

REM 检查Redis (可选)
echo [INFO] 正在检查Redis...
redis-cli ping >nul 2>&1
if errorlevel 1 (
    echo [WARN] Redis连接失败，请确保Redis已启动
) else (
    echo [SUCCESS] Redis连接成功
)

REM 编译
echo [INFO] 正在编译后端项目...
cd "%BACKEND_DIR%"

call mvnw.cmd clean install -DskipTests -q
if errorlevel 1 (
    echo [ERROR] 编译失败
    pause
    exit /b 1
)
echo [SUCCESS] 编译成功

REM 启动应用
echo [INFO] 正在启动Spring Boot应用...
echo.
echo 应用地址: http://localhost:8080
echo Swagger文档: http://localhost:8080/api/swagger-ui.html
echo DTO Schema验证: http://localhost:8080/api/v3/api-docs
echo.
echo 按 Ctrl+C 停止应用
echo.

REM 设置环境变量（如果需要）
if not defined DB_HOST set DB_HOST=localhost
if not defined DB_PORT set DB_PORT=3306
if not defined DB_NAME set DB_NAME=ev_charging_system
if not defined DB_USERNAME set DB_USERNAME=root
if not defined DB_PASSWORD set DB_PASSWORD=root123456
if not defined REDIS_HOST set REDIS_HOST=localhost
if not defined REDIS_PORT set REDIS_PORT=6379
if not defined JWT_SECRET set JWT_SECRET=your-secret-key-min-32-chars-long
if not defined SERVER_PORT set SERVER_PORT=8080

REM 启动应用
call mvnw.cmd spring-boot:run -q

echo [SUCCESS] 应用启动完成
pause
