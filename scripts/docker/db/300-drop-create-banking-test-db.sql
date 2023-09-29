--
-- Create Banking test DB
--

\c postgres;
DROP DATABASE IF EXISTS banking_test;
CREATE DATABASE banking_test OWNER banking;
\c banking_test;

--
-- banking_test permissions
--
GRANT CONNECT ON DATABASE banking_test TO banking;
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
