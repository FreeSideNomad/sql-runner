#!/bin/bash
#
# SQL Runner - Development startup script
# Starts Docker containers, kills conflicting processes, and runs the app
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
LOG_DIR="$PROJECT_DIR/logs"
LOG_FILE="$LOG_DIR/app.log"
APP_PORT=9090

# Database credentials for local Docker containers
export LOCAL_SQLSERVER_USER="sa"
export LOCAL_SQLSERVER_PASSWORD="SqlRunner123!"
export LOCAL_POSTGRES_USER="sqlrunner"
export LOCAL_POSTGRES_PASSWORD="SqlRunner123!"
export LOCAL_DB2_USER="db2inst1"
export LOCAL_DB2_PASSWORD="SqlRunner123!"

# SQL Server credentials for app datasource (used by dev profile)
export SQLSERVER_USER="sa"
export SQLSERVER_PASSWORD="SqlRunner123!"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Create logs directory
mkdir -p "$LOG_DIR"

# Kill any process running on the app port
log "Checking for processes on port $APP_PORT..."
PID=$(lsof -ti :$APP_PORT 2>/dev/null || true)
if [ -n "$PID" ]; then
    warn "Killing process(es) on port $APP_PORT: $PID"
    kill -9 $PID 2>/dev/null || true
    sleep 1
fi

# Start Docker containers
log "Starting Docker containers..."
cd "$PROJECT_DIR"
if ! docker-compose up -d 2>&1; then
    warn "Docker compose failed - continuing without external databases"
fi

# Wait a moment for containers to initialize
sleep 2

# Check Docker container status
log "Docker container status:"
docker-compose ps 2>/dev/null || warn "Could not check docker status"

# Build the application (skip tests for faster startup)
log "Building application..."
./mvnw clean compile -DskipTests -q

# Run the application
log "Starting SQL Runner on port $APP_PORT..."
log "Log file: $LOG_FILE"
log "Press Ctrl+C to stop"
echo ""
echo "============================================"
echo "  SQL Runner starting..."
echo "  URL: http://localhost:$APP_PORT"
echo "  Login: admin/admin, updater/updater, reader/reader"
echo "  H2 Console: http://localhost:$APP_PORT/h2-console"
echo "  Log: $LOG_FILE"
echo ""
echo "  Available Connections:"
echo "    - Local SQL Server (Docker) - localhost:1433"
echo "    - Local PostgreSQL (Docker) - localhost:5432"
echo "    - Local DB2 (Docker) - localhost:50000"
echo "============================================"
echo ""

# Run with output to both console and log file (dev profile uses SQL Server)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-Xmx512m" 2>&1 | tee "$LOG_FILE"
