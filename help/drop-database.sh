#!/usr/bin/env sh

DB_NAME=$1
if [ -z "$DB_NAME" ]
then
    echo "Specify databasename, usage"
    echo "$ drop-database <db>"
    exit 1;
fi
sed "s/my_database_name/$DB_NAME/" dropdatabase.template > dropdatabase.sql

psql -f dropdatabase.sql -U postgres $DB_NAME

# rm dropdatabase.sql