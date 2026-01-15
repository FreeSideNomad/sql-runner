#!/bin/bash
#
# Generate test data in Docker databases
# Usage: ./scripts/generate-test-data.sh [sqlserver|postgres|db2|all]
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Default credentials
export LOCAL_SQLSERVER_USER="sa"
export LOCAL_SQLSERVER_PASSWORD="SqlRunner123!"
export LOCAL_POSTGRES_USER="sqlrunner"
export LOCAL_POSTGRES_PASSWORD="SqlRunner123!"
export LOCAL_DB2_USER="db2inst1"
export LOCAL_DB2_PASSWORD="SqlRunner123!"

TARGET=${1:-all}

cd "$PROJECT_DIR"

echo "Generating test data for: $TARGET"
echo ""

# Run the generator via Maven test
./mvnw test -Dtest=TestDataGeneratorRunner#generate${TARGET^} -q 2>&1

echo ""
echo "Test data generation complete!"
