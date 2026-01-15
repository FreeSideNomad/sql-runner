#!/bin/bash
echo "Running pre-commit checks..."

# Check code formatting
echo "Checking code formatting..."
mvn spotless:check -q
if [ $? -ne 0 ]; then
    echo "Code formatting check failed. Run 'mvn spotless:apply' to fix."
    exit 1
fi

# Run unit tests
echo "Running unit tests..."
mvn test -q
if [ $? -ne 0 ]; then
    echo "Unit tests failed. Fix the tests before committing."
    exit 1
fi

echo "Pre-commit checks passed!"
