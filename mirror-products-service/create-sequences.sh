#!/bin/bash

# Database connection parameters
HOST="localhost"
PORT="5433"
DATABASE="mirror_mrp"
USER="mirror_user"

# Set password as environment variable
export PGPASSWORD="mirror_password"

# Create sequences
psql -h $HOST -p $PORT -U $USER -d $DATABASE -c "CREATE SEQUENCE IF NOT EXISTS cat_seq START 1;"
psql -h $HOST -p $PORT -U $USER -d $DATABASE -c "CREATE SEQUENCE IF NOT EXISTS com_seq START 1;"
psql -h $HOST -p $PORT -U $USER -d $DATABASE -c "CREATE SEQUENCE IF NOT EXISTS opt_seq START 1;"
psql -h $HOST -p $PORT -U $USER -d $DATABASE -c "CREATE SEQUENCE IF NOT EXISTS itv_seq START 1;"
psql -h $HOST -p $PORT -U $USER -d $DATABASE -c "CREATE SEQUENCE IF NOT EXISTS itc_seq START 1;"
psql -h $HOST -p $PORT -U $USER -d $DATABASE -c "CREATE SEQUENCE IF NOT EXISTS loc_seq START 1;"
psql -h $HOST -p $PORT -U $USER -d $DATABASE -c "CREATE SEQUENCE IF NOT EXISTS prd_seq START 1;"
psql -h $HOST -p $PORT -U $USER -d $DATABASE -c "CREATE SEQUENCE IF NOT EXISTS col_seq START 1;"
psql -h $HOST -p $PORT -U $USER -d $DATABASE -c "CREATE SEQUENCE IF NOT EXISTS cpr_seq START 1;"
psql -h $HOST -p $PORT -U $USER -d $DATABASE -c "CREATE SEQUENCE IF NOT EXISTS fil_seq START 1;"

echo "All sequences created successfully!"