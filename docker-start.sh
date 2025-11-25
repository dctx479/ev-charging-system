#!/bin/bash
# ============================================
# EV Charging System - Docker Startup Script (Linux/Mac)
# ============================================

set -e

echo "============================================"
echo "EV Charging System - Docker Startup"
echo "============================================"
echo

# Check if Docker is running
echo "[1/5] Checking Docker status..."
if ! docker info > /dev/null 2>&1; then
    echo "[ERROR] Docker is not running!"
    echo "Please start Docker and try again."
    exit 1
fi
echo "[OK] Docker is running"
echo

# Check if .env.docker exists
echo "[2/5] Checking environment configuration..."
if [ ! -f .env.docker ]; then
    echo "[WARNING] .env.docker not found, using default values"
else
    echo "[OK] Found .env.docker configuration"
fi
echo

# Stop existing containers
echo "[3/5] Stopping existing containers (if any)..."
docker-compose --env-file .env.docker down
echo

# Build and start containers
echo "[4/5] Building and starting all containers..."
echo "This may take several minutes on first run..."
docker-compose --env-file .env.docker up -d --build
echo

# Wait for services to be ready
echo "[5/5] Waiting for services to be ready..."
sleep 30

# Check container status
echo
echo "============================================"
echo "Container Status:"
echo "============================================"
docker-compose --env-file .env.docker ps
echo

# Show service URLs
echo "============================================"
echo "Service URLs:"
echo "============================================"
echo "[MySQL]          localhost:3306"
echo "[Redis]          localhost:6379"
echo "[RabbitMQ]       http://localhost:15672 (admin/admin123)"
echo "[Backend API]    http://localhost:8080/api"
echo "[AI Service]     http://localhost:5000"
echo "[User Frontend]  http://localhost:5173"
echo "[Admin Console]  http://localhost:5174"
echo "============================================"
echo

echo "[SUCCESS] All services are starting!"
echo "Please wait 1-2 minutes for all services to be fully ready."
echo
echo "To view logs: docker-compose logs -f [service-name]"
echo "To stop all:  docker-compose down"
