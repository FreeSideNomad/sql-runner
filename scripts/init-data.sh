#!/bin/bash
#
# Initialize databases with test data
# Usage: ./scripts/init-data.sh [--scale=N] [--db=sqlserver|postgres|db2|all]
#
# Options:
#   --scale=N   Number of customers to generate. Default: 1
#               Each customer gets ~10 accounts, each account ~500 transactions
#   --db=name   Target database (sqlserver|postgres|db2|all). Default: all
#
# Examples:
#   ./scripts/init-data.sh                     # Quick test (1 customer, ~5K txns)
#   ./scripts/init-data.sh --scale=100         # Medium (100 customers, ~500K txns)
#   ./scripts/init-data.sh --scale=10000       # Full (10K customers, ~50M txns)
#   ./scripts/init-data.sh --db=postgres       # Initialize only PostgreSQL
#
# This script will:
#   1. Stop and remove existing database containers
#   2. Remove database volumes (fresh start)
#   3. Rebuild and start containers (schema created via Docker init)
#   4. Wait for databases to be ready
#   5. Generate test data using TestDataGenerator
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Default options
SCALE=1
TARGET_DB="all"

# Parse arguments
for arg in "$@"; do
    case $arg in
        --scale=*)
            SCALE="${arg#*=}"
            ;;
        --db=*)
            TARGET_DB="${arg#*=}"
            ;;
        *)
            echo "Unknown option: $arg"
            echo "Usage: $0 [--scale=N] [--db=sqlserver|postgres|db2|all]"
            exit 1
            ;;
    esac
done

cd "$PROJECT_DIR"

echo "=============================================="
echo "SQL Runner Database Initialization"
echo "=============================================="
echo "Scale:     $SCALE customers"
echo "           ~$(( SCALE * 10 )) accounts"
echo "           ~$(( SCALE * 10 * 500 )) transactions"
echo "Target:    $TARGET_DB"
echo ""

# Function to wait for a database to be ready
wait_for_db() {
    local db=$1
    local max_attempts=60
    local attempt=1

    echo "Waiting for $db to be ready..."

    case $db in
        postgres)
            while [ $attempt -le $max_attempts ]; do
                if docker exec sqlrunner-postgres pg_isready -U sqlrunner -d sqlrunner >/dev/null 2>&1; then
                    echo "$db is ready!"
                    return 0
                fi
                echo "  Attempt $attempt/$max_attempts..."
                sleep 2
                ((attempt++))
            done
            ;;
        sqlserver)
            while [ $attempt -le $max_attempts ]; do
                if docker exec sqlrunner-sqlserver bash -c '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -Q "SELECT 1"' >/dev/null 2>&1; then
                    echo "$db is ready!"
                    return 0
                fi
                echo "  Attempt $attempt/$max_attempts..."
                sleep 2
                ((attempt++))
            done
            ;;
        db2)
            # DB2 takes longer to initialize - use more attempts with longer sleep
            max_attempts=120
            while [ $attempt -le $max_attempts ]; do
                if docker exec sqlrunner-db2 bash -c 'su - db2inst1 -c "db2 connect to TESTDB"' >/dev/null 2>&1; then
                    echo "$db is ready!"
                    return 0
                fi
                echo "  Attempt $attempt/$max_attempts..."
                sleep 5
                ((attempt++))
            done
            ;;
    esac

    echo "ERROR: $db did not become ready in time"
    return 1
}

# Function to initialize schema for a single database
init_schema() {
    local db=$1
    echo ""
    echo "Initializing schema for $db..."

    case $db in
        postgres)
            # Schema runs automatically from /docker-entrypoint-initdb.d
            # But we run it again in case tables don't exist
            docker exec sqlrunner-postgres psql -U sqlrunner -d sqlrunner -f /docker-entrypoint-initdb.d/01-schema.sql 2>&1 || true
            ;;
        sqlserver)
            # Run schema (includes database creation)
            docker exec sqlrunner-sqlserver bash -c '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -i /docker-entrypoint-initdb.d/01-schema.sql'
            ;;
        db2)
            # Run schema (DB2 uses uppercase database names)
            docker exec sqlrunner-db2 bash -c 'su - db2inst1 -c "db2 connect to TESTDB && db2 -tvf /var/custom/sql/schema.sql"' || true
            ;;
    esac

    echo "$db schema initialized"
}

# Function to show row counts for a database
show_counts() {
    local db=$1
    echo ""
    echo "Row counts for $db:"

    case $db in
        postgres)
            docker exec sqlrunner-postgres psql -U sqlrunner -d sqlrunner -c \
                "SELECT 'customers' as table_name, COUNT(*) FROM customers UNION ALL SELECT 'accounts', COUNT(*) FROM accounts UNION ALL SELECT 'transactions', COUNT(*) FROM transactions;"
            ;;
        sqlserver)
            docker exec sqlrunner-sqlserver bash -c '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -d sqlrunner -Q "SELECT '\''customers'\'' as tbl, COUNT(*) as cnt FROM customers UNION ALL SELECT '\''accounts'\'', COUNT(*) FROM accounts UNION ALL SELECT '\''transactions'\'', COUNT(*) FROM transactions"'
            ;;
        db2)
            docker exec sqlrunner-db2 bash -c 'su - db2inst1 -c "db2 connect to TESTDB && db2 \"SELECT '\''customers'\'' as tbl, COUNT(*) as cnt FROM customers UNION ALL SELECT '\''accounts'\'', COUNT(*) FROM accounts UNION ALL SELECT '\''transactions'\'', COUNT(*) FROM transactions\""'
            ;;
    esac
}

# Determine which databases to process
case $TARGET_DB in
    all)
        DBS="postgres sqlserver db2"
        ;;
    postgres|sqlserver|db2)
        DBS="$TARGET_DB"
        ;;
    *)
        echo "Unknown database: $TARGET_DB"
        exit 1
        ;;
esac

# Step 1: Stop containers
echo "Stopping containers..."
for db in $DBS; do
    docker-compose stop $db 2>/dev/null || true
done

# Step 2: Remove containers and volumes
echo "Removing containers and volumes..."
for db in $DBS; do
    docker-compose rm -f $db 2>/dev/null || true
    docker volume rm "sql-r_${db}-data" 2>/dev/null || true
done

# Step 3: Rebuild and start containers
echo "Starting containers..."
docker-compose build $DBS 2>/dev/null || true
docker-compose up -d $DBS

# Step 4: Wait for databases to be ready
for db in $DBS; do
    wait_for_db $db || exit 1
done

# Step 5: Initialize schemas
for db in $DBS; do
    init_schema $db
done

# Step 6: Generate test data using TestDataGenerator
echo ""
echo "=============================================="
echo "Generating test data (scale=$SCALE)..."
echo "=============================================="

# Set environment variables for database connections
export LOCAL_SQLSERVER_USER="sa"
export LOCAL_SQLSERVER_PASSWORD="SqlRunner123!"
export LOCAL_POSTGRES_USER="sqlrunner"
export LOCAL_POSTGRES_PASSWORD="SqlRunner123!"
export LOCAL_DB2_USER="db2inst1"
export LOCAL_DB2_PASSWORD="SqlRunner123!"
export SCALE="$SCALE"

# Run the TestDataGenerator via Maven
for db in $DBS; do
    echo ""
    echo "Generating data for $db..."
    db_capitalized="$(tr '[:lower:]' '[:upper:]' <<< ${db:0:1})${db:1}"
    ./mvnw test -Dtest="TestDataGeneratorRunner#generate${db_capitalized}" -Dscale="$SCALE" -q 2>&1 || echo "Warning: TestDataGenerator failed for $db"
done

# Step 7: Show final counts
echo ""
echo "=============================================="
echo "Initialization Complete - Row Counts"
echo "=============================================="

for db in $DBS; do
    show_counts $db
done

echo ""
echo "Done! Databases are ready for use."
