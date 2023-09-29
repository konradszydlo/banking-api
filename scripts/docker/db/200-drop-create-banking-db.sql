--
-- Create Banking DB

--
\c postgres;
DROP DATABASE IF EXISTS bankingdb;
CREATE DATABASE bankingdb OWNER banking;
\c bankingdb;

--
-- bankingdb permissions
--
GRANT CONNECT ON DATABASE bankingdb TO banking;
CREATE SCHEMA IF NOT EXISTS banking AUTHORIZATION banking;

--
-- Remove public permissions for ALL users (good practice)
--
REVOKE ALL ON SCHEMA public FROM PUBLIC;

--
-- All done
--
\c postgres;

--
-- END
--
