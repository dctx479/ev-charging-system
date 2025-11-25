@echo off
REM ============================================
REM EV Charging System - View Logs Script
REM ============================================

echo ============================================
echo EV Charging System - Service Logs
echo ============================================
echo.
echo Available services:
echo   - mysql
echo   - redis
echo   - rabbitmq
echo   - backend
echo   - ai-service
echo   - frontend-user
echo   - frontend-admin
echo.
echo Usage: docker-logs.bat [service-name]
echo Example: docker-logs.bat backend
echo.

if "%1"=="" (
    echo Showing logs for all services...
    docker-compose --env-file .env.docker logs -f
) else (
    echo Showing logs for %1...
    docker-compose --env-file .env.docker logs -f %1
)
