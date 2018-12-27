DO
\$do\$
BEGIN
   IF NOT EXISTS (
      SELECT 
      FROM   pg_catalog.pg_roles
      WHERE  rolname = '${dbuser}'
   ) THEN
      CREATE ROLE ${dbuser} LOGIN PASSWORD '${dbuser}';
   END IF;
END
\$do\$;

DROP DATABASE IF EXISTS ${db};
CREATE DATABASE ${db} WITH OWNER ${dbuser};