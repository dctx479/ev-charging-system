@echo off
REM ============================================
REM EV Charging System - Docker Stop Script
REM ============================================

echo ============================================
echo EV Charging System - Stopping Containers
echo ============================================
echo.

docker-compose --env-file .env.docker down

echo.
echo [SUCCESS] All containers stopped
pause
