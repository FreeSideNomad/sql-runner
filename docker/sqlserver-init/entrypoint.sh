#!/bin/bash
# Custom entrypoint for SQL Server with init script support

# Start SQL Server in background
/opt/mssql/bin/sqlservr &

# Wait for SQL Server to be ready
echo "Waiting for SQL Server to start..."
sleep 30

# Check if database already exists (init already done)
for i in {1..30}; do
    /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -Q "SELECT 1" &>/dev/null
    if [ $? -eq 0 ]; then
        echo "SQL Server is ready"
        break
    fi
    echo "Waiting for SQL Server... ($i/30)"
    sleep 2
done

# Check if init has already been done
DB_EXISTS=$(/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -Q "SET NOCOUNT ON; SELECT COUNT(*) FROM sys.databases WHERE name='sqlrunner'" -h -1 2>/dev/null | tr -d ' \n\r')
echo "DB_EXISTS check returned: '$DB_EXISTS'"

if [ "$DB_EXISTS" != "1" ]; then
    echo "Running initialization scripts..."
    
    # Create database
    /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -Q "CREATE DATABASE sqlrunner"
    
    # Run schema script
    if [ -f /docker-entrypoint-initdb.d/01-schema.sql ]; then
        echo "Running schema script..."
        /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -d sqlrunner -i /docker-entrypoint-initdb.d/01-schema.sql
    fi

    echo "Schema initialization complete!"
else
    echo "Database already initialized, skipping init scripts"
fi

# Keep container running
wait
