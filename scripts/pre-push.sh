#!/bin/bash
echo "Running pre-push checks..."

# Run full test suite with coverage
echo "Running tests with coverage check..."
mvn verify -Pcoverage -q
if [ $? -ne 0 ]; then
    echo "Tests or coverage check failed. Fix before pushing."
    exit 1
fi

echo "Pre-push checks passed!"
