SELECT * from pg_database where datname = 'taskmgr'

-- Disallow new connections
UPDATE pg_database SET datallowconn = 'false' WHERE datname = 'taskmgr';
ALTER DATABASE taskmgr CONNECTION LIMIT 1;

-- Terminate existing connections
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'taskmgr';

-- Drop database
DROP DATABASE taskmgr;