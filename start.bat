@echo off
chcp 65001 >nul
echo =========================================
echo   EV 充电站管理系统 - 快速启动脚本
echo =========================================
echo.

:MENU
echo 请选择操作:
echo [1] 启动后端服务 (Spring Boot)
echo [2] 启动前端用户端 (Vue3)
echo [3] 启动管理后台 (Vue3)
echo [4] 启动 AI 服务 (Flask)
echo [5] 启动 MySQL + Redis (Docker)
echo [6] 初始化数据库
echo [7] 查看服务状态
echo [0] 退出
echo.

set /p choice="请输入选项 [0-7]: "

if "%choice%"=="1" goto START_BACKEND
if "%choice%"=="2" goto START_FRONTEND_USER
if "%choice%"=="3" goto START_FRONTEND_ADMIN
if "%choice%"=="4" goto START_AI
if "%choice%"=="5" goto START_DOCKER
if "%choice%"=="6" goto INIT_DB
if "%choice%"=="7" goto CHECK_STATUS
if "%choice%"=="0" goto END
goto MENU

:START_BACKEND
echo.
echo [后端服务] 正在启动...
cd /d "%~dp0backend"
start "EV充电系统-后端服务" cmd /k "mvn spring-boot:run"
echo 后端服务已在新窗口启动！
echo 访问地址: http://localhost:8080/api
echo.
pause
goto MENU

:START_FRONTEND_USER
echo.
echo [用户端前端] 正在启动...
cd /d "%~dp0frontend-user"
start "EV充电系统-用户端" cmd /k "npm run dev"
echo 用户端前端已在新窗口启动！
echo 访问地址: http://localhost:5173
echo.
pause
goto MENU

:START_FRONTEND_ADMIN
echo.
echo [管理后台] 正在启动...
cd /d "%~dp0frontend-admin"
start "EV充电系统-管理后台" cmd /k "npm run dev"
echo 管理后台已在新窗口启动！
echo 访问地址: http://localhost:5174
echo.
pause
goto MENU

:START_AI
echo.
echo [AI服务] 正在启动...
cd /d "%~dp0ai-service"
start "EV充电系统-AI服务" cmd /k "python app.py"
echo AI服务已在新窗口启动！
echo 访问地址: http://localhost:5000
echo.
pause
goto MENU

:START_DOCKER
echo.
echo [Docker服务] 正在启动 MySQL 和 Redis...
echo 注意: 请确保已配置 .env 文件中的数据库密码
docker-compose up -d mysql redis
echo.
echo 等待服务启动（10秒）...
timeout /t 10 /nobreak >nul
docker-compose ps
echo.
pause
goto MENU

:INIT_DB
echo.
echo [数据库初始化]
echo 请确保 MySQL 已经启动！
echo.
echo 注意: 请从 .env 文件获取数据库密码
echo 命令格式: mysql -h localhost -P 3306 -u root -p^<password^> ^< database\init.sql
echo.
set /p db_password="请输入数据库密码: "
echo 正在初始化数据库...
mysql -h localhost -P 3306 -u root -p%db_password% < database\init.sql
if %errorlevel% equ 0 (
    echo 数据库初始化成功！
) else (
    echo 数据库初始化失败！请检查 MySQL 连接和密码。
)
echo.
pause
goto MENU

:CHECK_STATUS
echo.
echo [服务状态检查]
echo.

echo 1. 检查端口占用情况:
echo ----------------------------------------
netstat -an | findstr ":8080" | findstr "LISTENING" >nul
if %errorlevel% equ 0 (
    echo [√] 后端服务 (8080) - 运行中
) else (
    echo [×] 后端服务 (8080) - 未运行
)

netstat -an | findstr ":5173" | findstr "LISTENING" >nul
if %errorlevel% equ 0 (
    echo [√] 用户端前端 (5173) - 运行中
) else (
    echo [×] 用户端前端 (5173) - 未运行
)

netstat -an | findstr ":3306" | findstr "LISTENING" >nul
if %errorlevel% equ 0 (
    echo [√] MySQL (3306) - 运行中
) else (
    echo [×] MySQL (3306) - 未运行
)

netstat -an | findstr ":6379" | findstr "LISTENING" >nul
if %errorlevel% equ 0 (
    echo [√] Redis (6379) - 运行中
) else (
    echo [×] Redis (6379) - 未运行
)

echo.
echo 2. Docker 容器状态:
echo ----------------------------------------
docker-compose ps 2>nul
if %errorlevel% neq 0 (
    echo Docker 未运行或未安装
)

echo.
pause
goto MENU

:END
echo.
echo 感谢使用！
echo.
exit
