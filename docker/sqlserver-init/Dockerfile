# SQL Server with custom init script support
FROM mcr.microsoft.com/mssql/server:2022-latest

# Copy init scripts (schema only - test data generated separately)
COPY 01-schema.sql /docker-entrypoint-initdb.d/
COPY entrypoint.sh /usr/local/bin/

# Make entrypoint executable
USER root
RUN chmod +x /usr/local/bin/entrypoint.sh
USER mssql

# Use our custom entrypoint
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
