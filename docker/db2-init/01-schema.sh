#!/bin/bash
# DB2 schema initialization wrapper

# Connect to the database (DB2 uses uppercase names) and run schema
su - db2inst1 -c "db2 connect to TESTDB && db2 -tvf /var/custom/sql/schema.sql"
