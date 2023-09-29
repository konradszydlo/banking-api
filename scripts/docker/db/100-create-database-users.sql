--
-- Create banking users.
--
DO
$do$
BEGIN
        IF NOT EXISTS(SELECT FROM pg_catalog.pg_roles WHERE rolname = 'banking') THEN CREATE USER banking WITH ENCRYPTED PASSWORD 'api'; END IF;
END;
$do$;

--
-- END
--
